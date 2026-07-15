package com.procurementsaas.events;

/**
 * Kafka topic names, kept in one place so producers and consumers cannot drift apart.
 *
 * <p>Topics are named after the fact that happened, in the past tense — an event records
 * something that already occurred; it does not instruct anyone to do anything.
 */
public final class Topics {

    public static final String TENDER_PUBLISHED = "tender.published";
    public static final String TENDER_AWARDED = "tender.awarded";
    public static final String SUPPLIER_DEBARRED = "supplier.debarred";

    private Topics() {
    }
}
