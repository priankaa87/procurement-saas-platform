package com.procurementsaas.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Notification Service.
 *
 * <p>Reacts to domain events from across the platform and turns them into messages —
 * telling invited suppliers a tender is open, telling bidders whether they won or lost.
 *
 * <p>It is a pure consumer: no other service calls it, and it calls no other service. That
 * is the point of the event backbone — the Tender service announces what happened and owes
 * nothing to whoever listens, so notification can fail, be redeployed, or be replaced
 * without a tender ever noticing.
 */
@SpringBootApplication
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
