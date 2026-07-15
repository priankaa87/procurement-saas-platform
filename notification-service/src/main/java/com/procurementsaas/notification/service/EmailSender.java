package com.procurementsaas.notification.service;

/**
 * The delivery channel. Kept as an interface so the domain logic never depends on how a
 * message physically leaves the building — SMTP, SES, or a log line in development.
 */
public interface EmailSender {

    /**
     * @throws EmailDeliveryException if the channel rejects the message
     */
    void send(String recipient, String subject, String body);

    /** Thrown when a channel cannot accept a message. */
    class EmailDeliveryException extends RuntimeException {
        public EmailDeliveryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
