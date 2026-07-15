package com.procurementsaas.tender.messaging;

import com.procurementsaas.events.DomainEventPublisher;
import com.procurementsaas.events.TenderAwardedEvent;
import com.procurementsaas.events.TenderPublishedEvent;
import com.procurementsaas.events.Topics;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Forwards tender domain events to Kafka <em>after</em> the database transaction commits.
 *
 * <p>Publishing inside the transaction would announce tenders that later roll back, and
 * subscribers would act on something that never happened. Waiting for the commit means the
 * only failure mode is a lost event (crash between commit and send) rather than a false one.
 *
 * <p>{@link EventListener} is not used here on purpose — {@link TransactionalEventListener}
 * with {@link TransactionPhase#AFTER_COMMIT} is what gives that guarantee.
 */
@Component
public class TenderEventForwarder {

    private final DomainEventPublisher publisher;

    public TenderEventForwarder(DomainEventPublisher publisher) {
        this.publisher = publisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTenderPublished(TenderPublishedEvent event) {
        publisher.publish(Topics.TENDER_PUBLISHED, event.tenderCode(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTenderAwarded(TenderAwardedEvent event) {
        publisher.publish(Topics.TENDER_AWARDED, event.tenderCode(), event);
    }
}
