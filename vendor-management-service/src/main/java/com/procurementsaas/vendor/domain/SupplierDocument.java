package com.procurementsaas.vendor.domain;

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
import java.time.LocalDate;

/**
 * A compliance document (trade licence, tax certificate, …). The file itself lives in
 * object storage behind the Document service; {@code storageKey} is the pointer.
 */
@Entity
@Table(name = "supplier_document")
public class SupplierDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "doc_type", nullable = false, length = 50)
    private String docType;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt = Instant.now();

    @Column(name = "expires_at")
    private LocalDate expiresAt;

    protected SupplierDocument() {
    }

    public SupplierDocument(Supplier supplier, String docType, String fileName,
                            String storageKey, LocalDate expiresAt) {
        this.supplier = supplier;
        this.docType = docType;
        this.fileName = fileName;
        this.storageKey = storageKey;
        this.expiresAt = expiresAt;
    }

    /** True when the document has an expiry date that has already passed. */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDate.now());
    }

    public Long getId() { return id; }
    public Supplier getSupplier() { return supplier; }
    public String getDocType() { return docType; }
    public String getFileName() { return fileName; }
    public String getStorageKey() { return storageKey; }
    public Instant getUploadedAt() { return uploadedAt; }
    public LocalDate getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDate expiresAt) { this.expiresAt = expiresAt; }
}
