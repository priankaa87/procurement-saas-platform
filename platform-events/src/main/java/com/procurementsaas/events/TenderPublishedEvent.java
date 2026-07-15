package com.procurementsaas.events;

import java.time.Instant;
import java.util.List;

/**
 * Emitted once a tender becomes open for bidding.
 *
 * <p>Carries the invited supplier codes so consumers (notification, reporting) do not have
 * to call back into the Tender service to do their job — the event is self-contained.
 *
 * @param tenantId       the tenant the tender belongs to, so consumers stay tenant-aware
 * @param supplierCodes  suppliers invited to bid
 */
public record TenderPublishedEvent(
    String tenantId,
    String tenderCode,
    String title,
    Instant bidDeadline,
    List<String> supplierCodes,
    Instant occurredAt) {

    public TenderPublishedEvent {
        supplierCodes = supplierCodes == null ? List.of() : List.copyOf(supplierCodes);
    }
}
