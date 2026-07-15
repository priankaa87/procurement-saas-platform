package com.procurementsaas.tenantbilling.domain;

/** Invoice lifecycle. Issued invoices are never deleted — they are voided. */
public enum InvoiceStatus {
    ISSUED,
    PAID,
    VOID
}
