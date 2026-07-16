package com.procurementsaas.enlistment.domain;

/**
 * Standing of an enlistment.
 *
 * <p>{@code EXPIRED} is never stored — it is derived from the validity dates, so an
 * enlistment lapses on time rather than when a job gets round to it.
 */
public enum EnlistmentStatus {
    ACTIVE,
    EXPIRED,
    REVOKED
}
