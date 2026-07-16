package com.procurementsaas.contract.domain;

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
import java.time.Instant;

/**
 * A record that goods arrived. Append-only: a receipt is the evidence behind a payment, so
 * a mistake is corrected by a further entry, never by editing history.
 */
@Entity
@Table(name = "goods_receipt")
public class GoodsReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_line_id", nullable = false)
    private DeliveryLine deliveryLine;

    @Column(nullable = false, precision = 18, scale = 3)
    private BigDecimal quantity;

    @Column(name = "received_by", nullable = false, length = 100)
    private String receivedBy;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt = Instant.now();

    /** True if it arrived after the line's due date at the time of receipt. */
    @Column(nullable = false)
    private boolean late;

    @Column(length = 500)
    private String remarks;

    protected GoodsReceipt() {
    }

    public GoodsReceipt(DeliveryLine deliveryLine, BigDecimal quantity, String receivedBy,
                        boolean late, String remarks) {
        this.deliveryLine = deliveryLine;
        this.quantity = quantity;
        this.receivedBy = receivedBy;
        this.late = late;
        this.remarks = remarks;
    }

    public Long getId() { return id; }
    public DeliveryLine getDeliveryLine() { return deliveryLine; }
    public BigDecimal getQuantity() { return quantity; }
    public String getReceivedBy() { return receivedBy; }
    public Instant getReceivedAt() { return receivedAt; }
    public boolean isLate() { return late; }
    public String getRemarks() { return remarks; }
}
