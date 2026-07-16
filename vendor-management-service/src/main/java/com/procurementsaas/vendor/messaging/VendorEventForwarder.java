package com.procurementsaas.vendor.messaging;

import com.procurementsaas.events.DomainEventPublisher;
import com.procurementsaas.events.SupplierDebarredEvent;
import com.procurementsaas.events.Topics;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Announces vendor decisions to the rest of the platform once they are committed.
 *
 * <p>Debarment is the one that matters: it is a decision several services need to act on —
 * the supplier must be told, and their pre-qualifications withdrawn — and none of that
 * should be this service's problem, nor should any of it be able to fail a debarment.
 */
@Component
public class VendorEventForwarder {

    private final DomainEventPublisher publisher;

    public VendorEventForwarder(DomainEventPublisher publisher) {
        this.publisher = publisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSupplierDebarred(SupplierDebarredEvent event) {
        publisher.publish(Topics.SUPPLIER_DEBARRED, event.supplierCode(), event);
    }
}
