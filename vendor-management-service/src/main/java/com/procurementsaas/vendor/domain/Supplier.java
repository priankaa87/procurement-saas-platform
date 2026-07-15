package com.procurementsaas.vendor.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * A supplier (vendor) registered on the platform.
 *
 * <p>{@code countryIso2} and {@code categoryCodes} reference Master Data by code rather
 * than by foreign key — services own their own schemas, so cross-context references are
 * carried as stable business codes.
 */
@Entity
@Table(name = "supplier")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "legal_name", length = 250)
    private String legalName;

    @Column(length = 150)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SupplierStatus status = SupplierStatus.DRAFT;

    /** ISO 3166-1 alpha-2 code owned by the Master Data service. */
    @Column(name = "country_iso2", length = 2)
    private String countryIso2;

    /** Item category codes owned by the Master Data service. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "supplier_category",
        joinColumns = @JoinColumn(name = "supplier_id"))
    @Column(name = "category_code", nullable = false, length = 50)
    private Set<String> categoryCodes = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Supplier() {
    }

    public Supplier(String code, String name, String legalName, String email, String phone,
                    String taxId, String countryIso2) {
        this.code = code;
        this.name = name;
        this.legalName = legalName;
        this.email = email;
        this.phone = phone;
        this.taxId = taxId;
        this.countryIso2 = countryIso2;
    }

    /** Approves a draft or suspended supplier so it can participate in tenders. */
    public void activate() {
        if (status == SupplierStatus.DEBARRED) {
            throw new IllegalStateException(
                "Supplier " + code + " is debarred and must be reinstated before activation");
        }
        this.status = SupplierStatus.ACTIVE;
    }

    public void suspend() {
        if (status == SupplierStatus.DEBARRED) {
            throw new IllegalStateException("Supplier " + code + " is already debarred");
        }
        this.status = SupplierStatus.SUSPENDED;
    }

    /**
     * Applies a debarment decision. Call this only from the debarment process, which
     * owns the eligibility rules and writes the accompanying audit record.
     */
    public void markDebarred() {
        this.status = SupplierStatus.DEBARRED;
    }

    /** Lifts a debarment. Counterpart to {@link #markDebarred()}; same ownership rule. */
    public void markReinstated() {
        this.status = SupplierStatus.ACTIVE;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLegalName() { return legalName; }
    public void setLegalName(String legalName) { this.legalName = legalName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }
    public SupplierStatus getStatus() { return status; }
    public String getCountryIso2() { return countryIso2; }
    public void setCountryIso2(String countryIso2) { this.countryIso2 = countryIso2; }
    public Set<String> getCategoryCodes() { return categoryCodes; }
    public void setCategoryCodes(Set<String> categoryCodes) { this.categoryCodes = categoryCodes; }
    public Instant getCreatedAt() { return createdAt; }
}
