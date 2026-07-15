package com.procurementsaas.notification.domain;

/** Delivery state of a single notification. */
public enum NotificationStatus {
    /** Created, not yet handed to a channel. */
    PENDING,
    /** Accepted by the channel. */
    SENT,
    /** The channel rejected it; {@code error} explains why. */
    FAILED
}
