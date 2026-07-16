package com.procurementsaas.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Emitted when a tender is awarded.
 *
 * <p>Carries the winning amount as well as the winner, so that a consumer raising a
 * contract does not have to reach back into the Tender service to find out what was
 * actually agreed — and so that the figure is fixed at the moment of award rather than
 * re-read later.
 *
 * <p>Includes the unsuccessful bidders too: everyone who took part is entitled to know the
 * outcome, and consumers should not have to work out who lost.
 */
public record TenderAwardedEvent(
    String tenantId,
    String tenderCode,
    String title,
    String awardedSupplierCode,
    BigDecimal awardedAmount,
    String currencyCode,
    List<String> unsuccessfulSupplierCodes,
    Instant occurredAt) {

    public TenderAwardedEvent {
        unsuccessfulSupplierCodes =
            unsuccessfulSupplierCodes == null ? List.of() : List.copyOf(unsuccessfulSupplierCodes);
    }
}
