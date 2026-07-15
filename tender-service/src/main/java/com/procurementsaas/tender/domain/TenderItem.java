package com.procurementsaas.tender.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** A line item being procured. {@code itemCode}/{@code unitCode} are Master Data codes. */
@Entity
@Table(name = "tender_item")
public class TenderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tender_id", nullable = false)
    private Tender tender;

    @Column(name = "item_code", nullable = false, length = 50)
    private String itemCode;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 18, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_code", nullable = false, length = 30)
    private String unitCode;

    protected TenderItem() {
    }

    public TenderItem(Tender tender, String itemCode, String description, BigDecimal quantity,
                      String unitCode) {
        this.tender = tender;
        this.itemCode = itemCode;
        this.description = description;
        this.quantity = quantity;
        this.unitCode = unitCode;
    }

    public Long getId() { return id; }
    public Tender getTender() { return tender; }
    public String getItemCode() { return itemCode; }
    public String getDescription() { return description; }
    public BigDecimal getQuantity() { return quantity; }
    public String getUnitCode() { return unitCode; }
}
