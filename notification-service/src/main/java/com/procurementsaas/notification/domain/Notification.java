package com.procurementsaas.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One rendered message aimed at one recipient.
 *
 * <p>{@code eventKey} records which event produced it, which is what makes consumption
 * idempotent: Kafka redelivery is normal, and a supplier should not be emailed twice
 * because a consumer restarted mid-batch.
 */
@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Supplier code or user identifier the message is addressed to. */
    @Column(nullable = false, length = 100)
    private String recipient;

    @Column(name = "template_code", nullable = false, length = 60)
    private String templateCode;

    @Column(nullable = false, length = 250)
    private String subject;

    @Column(nullable = false, length = 4000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status = NotificationStatus.PENDING;

    /** Natural key of the originating event + recipient; unique, so replays are no-ops. */
    @Column(name = "event_key", nullable = false, length = 200)
    private String eventKey;

    @Column(length = 500)
    private String error;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "sent_at")
    private Instant sentAt;

    protected Notification() {
    }

    public Notification(String recipient, String templateCode, String subject, String body,
                        String eventKey) {
        this.recipient = recipient;
        this.templateCode = templateCode;
        this.subject = subject;
        this.body = body;
        this.eventKey = eventKey;
    }

    public void markSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = Instant.now();
        this.error = null;
    }

    public void markFailed(String error) {
        this.status = NotificationStatus.FAILED;
        this.error = error != null && error.length() > 500 ? error.substring(0, 500) : error;
    }

    public Long getId() { return id; }
    public String getRecipient() { return recipient; }
    public String getTemplateCode() { return templateCode; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public NotificationStatus getStatus() { return status; }
    public String getEventKey() { return eventKey; }
    public String getError() { return error; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSentAt() { return sentAt; }
}
