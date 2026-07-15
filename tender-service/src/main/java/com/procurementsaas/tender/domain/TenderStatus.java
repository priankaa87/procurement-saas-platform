package com.procurementsaas.tender.domain;

/**
 * Tender lifecycle states.
 *
 * <pre>
 *   DRAFT ──publish──▶ PUBLISHED ──open (after deadline)──▶ UNDER_EVALUATION ──award──▶ AWARDED
 *     │                    │                                       │
 *     └────────────────────┴───────────── cancel ─────────────────-┘  ──▶ CANCELLED
 * </pre>
 *
 * Bids may only be submitted while PUBLISHED and before the deadline, and may only be
 * read once the tender reaches UNDER_EVALUATION (i.e. after it has been opened).
 */
public enum TenderStatus {
    /** Being prepared; not visible to suppliers. */
    DRAFT,
    /** Open for bidding until the deadline. Bids are sealed. */
    PUBLISHED,
    /** Opened after the deadline; bids are now readable and under evaluation. */
    UNDER_EVALUATION,
    /** A supplier has been awarded the tender. */
    AWARDED,
    /** Abandoned before award. */
    CANCELLED
}
