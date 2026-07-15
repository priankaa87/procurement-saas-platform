package com.procurementsaas.events;

import java.time.Instant;
import java.util.List;

/**
 * Emitted when a tender is awarded.
 *
 * <p>Includes the unsuccessful bidders as well as the winner: everyone who took part is
 * entitled to know the outcome, and consumers should not have to work out who lost.
 */
public record TenderAwardedEvent(
    String tenantId,
    String tenderCode,
    String title,
    String awardedSupplierCode,
    List<String> unsuccessfulSupplierCodes,
    Instant occurredAt) {

    public TenderAwardedEvent {
        unsuccessfulSupplierCodes =
            unsuccessfulSupplierCodes == null ? List.of() : List.copyOf(unsuccessfulSupplierCodes);
    }
}
