package com.procurementsaas.events;

import java.time.Instant;
import java.time.LocalDate;

/** Emitted when a supplier is debarred and may no longer participate. */
public record SupplierDebarredEvent(
    String tenantId,
    String supplierCode,
    String supplierName,
    String reason,
    LocalDate debarredUntil,
    Instant occurredAt) {
}
