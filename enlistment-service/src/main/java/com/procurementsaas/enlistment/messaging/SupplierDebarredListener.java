package com.procurementsaas.enlistment.messaging;

import com.procurementsaas.enlistment.service.EnlistmentService;
import com.procurementsaas.events.SupplierDebarredEvent;
import com.procurementsaas.events.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Withdraws a debarred supplier's pre-qualifications.
 *
 * <p>This is the whole argument for the event backbone in one place. Vendor Management
 * decides a supplier is debarred; it should not also have to know that pre-qualification
 * exists, that notifications exist, or that anything else cares. It states what happened,
 * and the services that care react.
 *
 * <p>Revoking is idempotent, so a redelivered event simply finds nothing left to withdraw.
 */
@Component
public class SupplierDebarredListener {

    private static final Logger log = LoggerFactory.getLogger(SupplierDebarredListener.class);

    private final EnlistmentService enlistmentService;

    public SupplierDebarredListener(EnlistmentService enlistmentService) {
        this.enlistmentService = enlistmentService;
    }

    @KafkaListener(topics = Topics.SUPPLIER_DEBARRED, groupId = "enlistment-service")
    public void onSupplierDebarred(SupplierDebarredEvent event) {
        log.info("Supplier {} debarred; withdrawing pre-qualifications", event.supplierCode());
        enlistmentService.revokeAllFor(event.supplierCode(),
            "Supplier debarred: " + event.reason());
    }
}
