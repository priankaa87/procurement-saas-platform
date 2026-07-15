package com.procurementsaas.tender.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A tender (procurement competition).
 *
 * <p>The lifecycle rules live here rather than in the service, because they are invariants
 * of the tender itself: you cannot publish an empty tender, cannot open one before its
 * deadline, and cannot award one that was never opened.
 */
@Entity
@Table(name = "tender")
public class Tender {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TenderStatus status = TenderStatus.DRAFT;

    /** Currency code owned by the Master Data service. */
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "bid_deadline", nullable = false)
    private Instant bidDeadline;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "opened_at")
    private Instant openedAt;

    /** Supplier code of the winner; set on award. */
    @Column(name = "awarded_supplier_code", length = 50)
    private String awardedSupplierCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Tender() {
    }

    public Tender(String code, String title, String description, String currencyCode,
                  Instant bidDeadline) {
        this.code = code;
        this.title = title;
        this.description = description;
        this.currencyCode = currencyCode;
        this.bidDeadline = bidDeadline;
    }

    /**
     * Publishes the tender so invited suppliers can bid.
     *
     * @param itemCount number of line items; a tender with nothing to buy cannot be published
     */
    public void publish(long itemCount) {
        if (status != TenderStatus.DRAFT) {
            throw new IllegalStateException("Only a DRAFT tender can be published: " + code);
        }
        if (itemCount == 0) {
            throw new IllegalStateException("Cannot publish a tender with no items: " + code);
        }
        if (bidDeadline.isBefore(Instant.now())) {
            throw new IllegalStateException("Cannot publish with a deadline in the past: " + code);
        }
        this.status = TenderStatus.PUBLISHED;
        this.publishedAt = Instant.now();
    }

    /**
     * Opens the tender after the deadline, unsealing the bids for evaluation.
     * Opening early would expose sealed bids while bidding is still possible.
     */
    public void open() {
        if (status != TenderStatus.PUBLISHED) {
            throw new IllegalStateException("Only a PUBLISHED tender can be opened: " + code);
        }
        if (Instant.now().isBefore(bidDeadline)) {
            throw new IllegalStateException("Cannot open before the bid deadline: " + code);
        }
        this.status = TenderStatus.UNDER_EVALUATION;
        this.openedAt = Instant.now();
    }

    /** True once the tender has been opened and bids may be disclosed. */
    public boolean bidsAreSealed() {
        return openedAt == null;
    }

    /** True while suppliers may still submit bids. */
    public boolean acceptingBids() {
        return status == TenderStatus.PUBLISHED && Instant.now().isBefore(bidDeadline);
    }

    public void award(String supplierCode) {
        if (status != TenderStatus.UNDER_EVALUATION) {
            throw new IllegalStateException(
                "Only a tender under evaluation can be awarded: " + code);
        }
        this.awardedSupplierCode = supplierCode;
        this.status = TenderStatus.AWARDED;
    }

    public void cancel() {
        if (status == TenderStatus.AWARDED) {
            throw new IllegalStateException("An awarded tender cannot be cancelled: " + code);
        }
        this.status = TenderStatus.CANCELLED;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public TenderStatus getStatus() { return status; }
    public String getCurrencyCode() { return currencyCode; }
    public Instant getBidDeadline() { return bidDeadline; }
    public void setBidDeadline(Instant bidDeadline) { this.bidDeadline = bidDeadline; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getOpenedAt() { return openedAt; }
    public String getAwardedSupplierCode() { return awardedSupplierCode; }
    public Instant getCreatedAt() { return createdAt; }
}
