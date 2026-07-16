package com.procurementsaas.reporting.domain;

/**
 * Report job lifecycle.
 *
 * <pre>
 *   QUEUED ──picked up──▶ RUNNING ──▶ COMPLETED
 *                            └───────▶ FAILED
 * </pre>
 */
public enum JobStatus {
    /** Accepted, waiting for a worker. */
    QUEUED,
    RUNNING,
    /** The file exists and can be downloaded. */
    COMPLETED,
    /** Rendering failed; the reason is on the job. */
    FAILED
}
