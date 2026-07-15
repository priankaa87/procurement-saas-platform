package com.procurementsaas.notification.service;

import com.procurementsaas.common.web.NotFoundException;
import com.procurementsaas.notification.domain.Notification;
import com.procurementsaas.notification.domain.NotificationStatus;
import com.procurementsaas.notification.domain.NotificationTemplate;
import com.procurementsaas.notification.dto.Dtos.NotificationDto;
import com.procurementsaas.notification.dto.Dtos.TemplateDto;
import com.procurementsaas.notification.dto.Dtos.UpdateTemplateRequest;
import com.procurementsaas.notification.repo.NotificationRepository;
import com.procurementsaas.notification.repo.NotificationTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/** Renders templates into notifications and hands them to the delivery channel. */
@Service
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final EmailSender emailSender;

    public NotificationService(NotificationRepository notificationRepository,
                               NotificationTemplateRepository templateRepository,
                               EmailSender emailSender) {
        this.notificationRepository = notificationRepository;
        this.templateRepository = templateRepository;
        this.emailSender = emailSender;
    }

    /**
     * Creates and dispatches one notification.
     *
     * <p>Skips silently if {@code eventKey} was already handled — Kafka redelivery is
     * routine and must not produce a duplicate message.
     *
     * <p>A delivery failure is recorded on the notification rather than thrown: the event
     * was still handled correctly, and re-consuming the whole event because an SMTP server
     * blipped would spam everyone else on the same event.
     */
    public void createAndSend(String recipient, String templateCode, Map<String, String> variables,
                              String eventKey) {
        if (notificationRepository.existsByEventKey(eventKey)) {
            log.debug("Skipping already-handled event {}", eventKey);
            return;
        }
        NotificationTemplate template = templateRepository.findByCode(templateCode)
            .orElseThrow(() -> new NotFoundException("Template not found: " + templateCode));

        String subject = TemplateRenderer.render(template.getSubject(), variables);
        String body = TemplateRenderer.render(template.getBody(), variables);

        Notification notification = new Notification(recipient, templateCode, subject, body, eventKey);
        try {
            emailSender.send(recipient, subject, body);
            notification.markSent();
        } catch (RuntimeException ex) {
            log.warn("Delivery failed for {} ({})", recipient, eventKey, ex);
            notification.markFailed(ex.getMessage());
        }
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> list(String recipient, String status) {
        List<Notification> notifications;
        if (recipient != null) {
            notifications = notificationRepository.findByRecipientOrderByCreatedAtDesc(recipient);
        } else if (status != null) {
            notifications = notificationRepository.findByStatusOrderByCreatedAtDesc(parseStatus(status));
        } else {
            notifications = notificationRepository.findAll();
        }
        return notifications.stream().map(NotificationService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<TemplateDto> listTemplates() {
        return templateRepository.findAll().stream()
            .map(t -> new TemplateDto(t.getId(), t.getCode(), t.getSubject(), t.getBody()))
            .toList();
    }

    /** Templates are business content and can be edited without a redeploy. */
    public TemplateDto updateTemplate(String code, UpdateTemplateRequest request) {
        NotificationTemplate template = templateRepository.findByCode(code)
            .orElseThrow(() -> new NotFoundException("Template not found: " + code));
        template.setSubject(request.subject());
        template.setBody(request.body());
        NotificationTemplate saved = templateRepository.save(template);
        return new TemplateDto(saved.getId(), saved.getCode(), saved.getSubject(), saved.getBody());
    }

    /** Retries a failed notification through the channel again. */
    public NotificationDto retry(Long id) {
        Notification notification = notificationRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Notification not found: " + id));
        if (notification.getStatus() == NotificationStatus.SENT) {
            throw new IllegalStateException("Notification was already sent: " + id);
        }
        try {
            emailSender.send(notification.getRecipient(), notification.getSubject(),
                notification.getBody());
            notification.markSent();
        } catch (RuntimeException ex) {
            notification.markFailed(ex.getMessage());
        }
        return toDto(notificationRepository.save(notification));
    }

    private static NotificationDto toDto(Notification n) {
        return new NotificationDto(n.getId(), n.getRecipient(), n.getTemplateCode(),
            n.getSubject(), n.getBody(), n.getStatus().name(), n.getError(), n.getCreatedAt(),
            n.getSentAt());
    }

    private static NotificationStatus parseStatus(String status) {
        try {
            return NotificationStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown notification status: " + status);
        }
    }
}
