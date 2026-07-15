package com.procurementsaas.tender.domain;

/** Where an invited supplier stands in a tender. */
public enum ParticipantStatus {
    /** Invited but has not yet bid. */
    INVITED,
    /** Has submitted a bid. */
    BID_SUBMITTED,
    /** Pulled out before the deadline. */
    WITHDRAWN
}
