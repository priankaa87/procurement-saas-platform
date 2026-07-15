package com.procurementsaas.tenantbilling.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** A customer organisation on the platform, and the schema its data lives in. */
@Entity
@Table(name = "tenant")
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_key", nullable = false, unique = true, length = 30)
    private String tenantKey;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "schema_name", nullable = false, unique = true, length = 40)
    private String schemaName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TenantStatus status = TenantStatus.PENDING;

    @Column(name = "plan_code", nullable = false, length = 40)
    private String planCode;

    @Column(name = "contact_email", length = 150)
    private String contactEmail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "activated_at")
    private Instant activatedAt;

    protected Tenant() {
    }

    public Tenant(String tenantKey, String name, String planCode, String contactEmail) {
        TenantKey.validate(tenantKey);
        this.tenantKey = tenantKey;
        this.schemaName = TenantKey.schemaFor(tenantKey);
        this.name = name;
        this.planCode = planCode;
        this.contactEmail = contactEmail;
    }

    /** Called once the schema exists — a tenant is not usable before then. */
    public void markProvisioned() {
        if (status != TenantStatus.PENDING) {
            throw new IllegalStateException("Tenant is already provisioned: " + tenantKey);
        }
        this.status = TenantStatus.ACTIVE;
        this.activatedAt = Instant.now();
    }

    public void suspend() {
        if (status == TenantStatus.CANCELLED) {
            throw new IllegalStateException("A cancelled tenant cannot be suspended: " + tenantKey);
        }
        this.status = TenantStatus.SUSPENDED;
    }

    public void reactivate() {
        if (status != TenantStatus.SUSPENDED) {
            throw new IllegalStateException("Only a suspended tenant can be reactivated: " + tenantKey);
        }
        this.status = TenantStatus.ACTIVE;
    }

    public void cancel() {
        this.status = TenantStatus.CANCELLED;
    }

    public void changePlan(String planCode) {
        if (status == TenantStatus.CANCELLED) {
            throw new IllegalStateException("A cancelled tenant cannot change plan: " + tenantKey);
        }
        this.planCode = planCode;
    }

    public boolean isUsable() {
        return status == TenantStatus.ACTIVE;
    }

    public Long getId() { return id; }
    public String getTenantKey() { return tenantKey; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSchemaName() { return schemaName; }
    public TenantStatus getStatus() { return status; }
    public String getPlanCode() { return planCode; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getActivatedAt() { return activatedAt; }
}
