package com.procurementsaas.contract.domain;

/** How much of a delivery line has arrived. Derived from receipts, never set by hand. */
public enum DeliveryStatus {
    PENDING,
    PARTIAL,
    DELIVERED
}
