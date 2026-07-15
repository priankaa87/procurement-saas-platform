package com.procurementsaas.notification.messaging;

import com.procurementsaas.events.SupplierDebarredEvent;
import com.procurementsaas.events.TenderAwardedEvent;
import com.procurementsaas.events.TenderPublishedEvent;
import com.procurementsaas.events.Topics;
import com.procurementsaas.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * Turns domain events into notifications.
 *
 * <p>Each recipient gets their own {@code eventKey}, so redelivery of an event that has
 * already been partly processed resumes rather than duplicating what was already sent.
 */
@Component
public class TenderEventListener {

    private static final Logger log = LoggerFactory.getLogger(TenderEventListener.class);
    private static final DateTimeFormatter DEADLINE_FORMAT =
        DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private final NotificationService notificationService;

    public TenderEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = Topics.TENDER_PUBLISHED, groupId = "notification-service")
    public void onTenderPublished(TenderPublishedEvent event) {
        log.info("Tender published: {} ({} suppliers invited)",
            event.tenderCode(), event.supplierCodes().size());

        Map<String, String> variables = Map.of(
            "tenderCode", event.tenderCode(),
            "tenderTitle", event.title(),
            "bidDeadline", DEADLINE_FORMAT.format(event.bidDeadline()));

        for (String supplierCode : event.supplierCodes()) {
            notificationService.createAndSend(supplierCode, "TENDER_PUBLISHED", variables,
                key(Topics.TENDER_PUBLISHED, event.tenderCode(), supplierCode));
        }
    }

    @KafkaListener(topics = Topics.TENDER_AWARDED, groupId = "notification-service")
    public void onTenderAwarded(TenderAwardedEvent event) {
        log.info("Tender awarded: {} -> {}", event.tenderCode(), event.awardedSupplierCode());

        Map<String, String> variables = Map.of(
            "tenderCode", event.tenderCode(),
            "tenderTitle", event.title(),
            "supplierCode", event.awardedSupplierCode());

        notificationService.createAndSend(event.awardedSupplierCode(), "TENDER_AWARDED_WINNER",
            variables, key(Topics.TENDER_AWARDED, event.tenderCode(), event.awardedSupplierCode()));

        // Everyone who bid is told the outcome, not just the winner.
        for (String supplierCode : event.unsuccessfulSupplierCodes()) {
            notificationService.createAndSend(supplierCode, "TENDER_AWARDED_UNSUCCESSFUL",
                variables, key(Topics.TENDER_AWARDED, event.tenderCode(), supplierCode));
        }
    }

    @KafkaListener(topics = Topics.SUPPLIER_DEBARRED, groupId = "notification-service")
    public void onSupplierDebarred(SupplierDebarredEvent event) {
        log.info("Supplier debarred: {}", event.supplierCode());

        Map<String, String> variables = Map.of(
            "supplierCode", event.supplierCode(),
            "reason", event.reason());

        notificationService.createAndSend(event.supplierCode(), "SUPPLIER_DEBARRED", variables,
            key(Topics.SUPPLIER_DEBARRED, event.supplierCode(), event.occurredAt().toString()));
    }

    private static String key(String topic, String subject, String recipient) {
        return topic + ":" + subject + ":" + recipient;
    }
}
