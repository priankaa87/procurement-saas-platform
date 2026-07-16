package com.procurementsaas.reporting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** One request to produce a report, and what became of it. */
@Entity
@Table(name = "report_job")
public class ReportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "definition_code", nullable = false, length = 60)
    private String definitionCode;

    /** Filters, as submitted. Kept verbatim so a report can be explained or re-run later. */
    @Column(name = "parameters", length = 2000)
    private String parameters;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status = JobStatus.QUEUED;

    @Column(name = "requested_by", nullable = false, length = 100)
    private String requestedBy;

    /** The tenant the report was run for; a report must never span tenants. */
    @Column(name = "tenant_id", nullable = false, length = 63)
    private String tenantId;

    @Column(name = "storage_key", length = 300)
    private String storageKey;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "row_count")
    private Integer rowCount;

    @Column(length = 1000)
    private String error;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected ReportJob() {
    }

    public ReportJob(String definitionCode, String parameters, String requestedBy,
                     String tenantId) {
        this.definitionCode = definitionCode;
        this.parameters = parameters;
        this.requestedBy = requestedBy;
        this.tenantId = tenantId;
    }

    public void markRunning() {
        this.status = JobStatus.RUNNING;
        this.startedAt = Instant.now();
    }

    public void markCompleted(String storageKey, long sizeBytes, int rowCount) {
        this.status = JobStatus.COMPLETED;
        this.storageKey = storageKey;
        this.sizeBytes = sizeBytes;
        this.rowCount = rowCount;
        this.completedAt = Instant.now();
        this.error = null;
    }

    /**
     * Records why a report could not be produced.
     *
     * <p>A failure is data, not an exception thrown into the void: whoever asked for the
     * report needs to be told what went wrong, possibly hours later.
     */
    public void markFailed(String error) {
        this.status = JobStatus.FAILED;
        this.completedAt = Instant.now();
        this.error = error != null && error.length() > 1000 ? error.substring(0, 1000) : error;
    }

    public boolean isDownloadable() {
        return status == JobStatus.COMPLETED && storageKey != null;
    }

    public Long getId() { return id; }
    public String getDefinitionCode() { return definitionCode; }
    public String getParameters() { return parameters; }
    public JobStatus getStatus() { return status; }
    public String getRequestedBy() { return requestedBy; }
    public String getTenantId() { return tenantId; }
    public String getStorageKey() { return storageKey; }
    public Long getSizeBytes() { return sizeBytes; }
    public Integer getRowCount() { return rowCount; }
    public String getError() { return error; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
}
