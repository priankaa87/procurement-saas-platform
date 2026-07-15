package com.procurementsaas.tenantbilling.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One metered usage delta for a tenant, e.g. {@code users +1}.
 *
 * <p>Stored as an append-only ledger rather than a running total: a counter you can only
 * increment tells you nothing about when or why it moved, and totals derived from a ledger
 * can be recomputed if a rule changes.
 */
@Entity
@Table(name = "usage_record")
public class UsageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 40)
    private String metric;

    @Column(nullable = false)
    private long quantity;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt = Instant.now();

    protected UsageRecord() {
    }

    public UsageRecord(Tenant tenant, String metric, long quantity) {
        this.tenant = tenant;
        this.metric = metric;
        this.quantity = quantity;
    }

    public Long getId() { return id; }
    public Tenant getTenant() { return tenant; }
    public String getMetric() { return metric; }
    public long getQuantity() { return quantity; }
    public Instant getRecordedAt() { return recordedAt; }
}
