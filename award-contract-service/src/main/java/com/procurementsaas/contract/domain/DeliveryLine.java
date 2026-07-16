package com.procurementsaas.contract.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One scheduled delivery: what is owed, how much, and by when.
 *
 * <p>{@code receivedQuantity} and {@code status} are maintained here as goods arrive, so
 * that "what is still outstanding" is a column rather than a sum computed on every read.
 */
@Entity
@Table(name = "delivery_line",
    uniqueConstraints = @UniqueConstraint(columnNames = {"work_order_id", "line_no"}))
public class DeliveryLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    /** Item code owned by the Master Data service. */
    @Column(name = "item_code", nullable = false, length = 50)
    private String itemCode;

    @Column(name = "ordered_quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal orderedQuantity;

    @Column(name = "received_quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal receivedQuantity = BigDecimal.ZERO;

    @Column(name = "unit_code", nullable = false, length = 30)
    private String unitCode;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryStatus status = DeliveryStatus.PENDING;

    protected DeliveryLine() {
    }

    public DeliveryLine(WorkOrder workOrder, int lineNo, String itemCode,
                        BigDecimal orderedQuantity, String unitCode, LocalDate dueDate) {
        if (orderedQuantity == null || orderedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Ordered quantity must be greater than zero");
        }
        this.workOrder = workOrder;
        this.lineNo = lineNo;
        this.itemCode = itemCode;
        this.orderedQuantity = orderedQuantity;
        this.unitCode = unitCode;
        this.dueDate = dueDate;
    }

    /**
     * Records goods arriving against this line.
     *
     * <p>Refuses to accept more than was ordered: a receipt is what authorises payment, so
     * over-receipting is how a buyer pays for goods it never agreed to buy. If a supplier
     * genuinely ships more, the order must be varied first.
     */
    public void receive(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Received quantity must be greater than zero");
        }
        BigDecimal newTotal = receivedQuantity.add(quantity);
        if (newTotal.compareTo(orderedQuantity) > 0) {
            throw new IllegalArgumentException(
                "Cannot receive " + quantity + " " + unitCode + " on line " + lineNo
                    + ": ordered " + orderedQuantity + ", already received " + receivedQuantity);
        }
        this.receivedQuantity = newTotal;
        this.status = newTotal.compareTo(orderedQuantity) == 0
            ? DeliveryStatus.DELIVERED
            : DeliveryStatus.PARTIAL;
    }

    /** Extends the deadline. Only meaningful while something is still outstanding. */
    public void extendTo(LocalDate newDueDate) {
        if (status == DeliveryStatus.DELIVERED) {
            throw new IllegalStateException("Line " + lineNo + " is already fully delivered");
        }
        if (!newDueDate.isAfter(dueDate)) {
            throw new IllegalArgumentException(
                "New due date " + newDueDate + " must be later than the current " + dueDate);
        }
        this.dueDate = newDueDate;
    }

    public boolean isFullyDelivered() {
        return status == DeliveryStatus.DELIVERED;
    }

    /** Outstanding and past its due date. */
    public boolean isOverdue() {
        return !isFullyDelivered() && LocalDate.now().isAfter(dueDate);
    }

    public BigDecimal outstandingQuantity() {
        return orderedQuantity.subtract(receivedQuantity);
    }

    public Long getId() { return id; }
    public WorkOrder getWorkOrder() { return workOrder; }
    public int getLineNo() { return lineNo; }
    public String getItemCode() { return itemCode; }
    public BigDecimal getOrderedQuantity() { return orderedQuantity; }
    public BigDecimal getReceivedQuantity() { return receivedQuantity; }
    public String getUnitCode() { return unitCode; }
    public LocalDate getDueDate() { return dueDate; }
    public DeliveryStatus getStatus() { return status; }
}
