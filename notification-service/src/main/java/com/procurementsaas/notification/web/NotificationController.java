package com.procurementsaas.notification.web;

import com.procurementsaas.notification.dto.Dtos.NotificationDto;
import com.procurementsaas.notification.dto.Dtos.TemplateDto;
import com.procurementsaas.notification.dto.Dtos.UpdateTemplateRequest;
import com.procurementsaas.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/notifications")
    @PreAuthorize("hasAuthority('FEATURE_NOTIFICATION_VIEW')")
    public List<NotificationDto> list(@RequestParam(required = false) String recipient,
                                      @RequestParam(required = false) String status) {
        return notificationService.list(recipient, status);
    }

    @PostMapping("/notifications/{id}/retry")
    @PreAuthorize("hasAuthority('FEATURE_NOTIFICATION_MANAGE')")
    public NotificationDto retry(@PathVariable Long id) {
        return notificationService.retry(id);
    }

    @GetMapping("/templates")
    @PreAuthorize("hasAuthority('FEATURE_NOTIFICATION_VIEW')")
    public List<TemplateDto> listTemplates() {
        return notificationService.listTemplates();
    }

    @PutMapping("/templates/{code}")
    @PreAuthorize("hasAuthority('FEATURE_NOTIFICATION_MANAGE')")
    public TemplateDto updateTemplate(@PathVariable String code,
                                      @Valid @RequestBody UpdateTemplateRequest request) {
        return notificationService.updateTemplate(code, request);
    }
}
