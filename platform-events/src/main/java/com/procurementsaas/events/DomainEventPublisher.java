package com.procurementsaas.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Publishes domain events to Kafka.
 *
 * <p>Keyed by a business identifier (tender code, supplier code) so that all events about
 * the same thing land on the same partition and are therefore consumed in order.
 *
 * <p><strong>Delivery semantics:</strong> callers should publish only after their database
 * transaction has committed, so a rolled-back change never announces itself. The cost is
 * that a crash between commit and send loses the event — acceptable for notifications.
 * Anything requiring guaranteed delivery should move to a transactional outbox.
 */
public class DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DomainEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public DomainEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String topic, String key, Object event) {
        kafkaTemplate.send(topic, key, event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish {} to {} (key={})",
                    event.getClass().getSimpleName(), topic, key, ex);
            } else {
                log.debug("Published {} to {} (key={})",
                    event.getClass().getSimpleName(), topic, key);
            }
        });
    }
}
