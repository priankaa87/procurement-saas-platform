package com.procurementsaas.contract.service;

import com.procurementsaas.contract.domain.Award;
import com.procurementsaas.contract.domain.DeliveryLine;
import com.procurementsaas.contract.domain.GoodsReceipt;
import com.procurementsaas.contract.domain.WorkOrder;
import com.procurementsaas.contract.dto.Dtos.AwardDto;
import com.procurementsaas.contract.dto.Dtos.DeliveryLineDto;
import com.procurementsaas.contract.dto.Dtos.GoodsReceiptDto;
import com.procurementsaas.contract.dto.Dtos.WorkOrderDto;

/** Maps contract entities to API DTOs. */
public final class ContractMapper {

    private ContractMapper() {
    }

    public static AwardDto toDto(Award a) {
        return new AwardDto(a.getId(), a.getTenderCode(), a.getTenderTitle(), a.getSupplierCode(),
            a.getAmount(), a.getCurrencyCode(), a.getStatus().name(), a.getIssuedAt(),
            a.getRespondBy(), a.getRespondedAt(), a.getDeclineReason());
    }

    public static WorkOrderDto toDto(WorkOrder w, long lineCount) {
        return new WorkOrderDto(w.getId(), w.getCode(), w.getAward().getTenderCode(),
            w.getAward().getSupplierCode(), w.getStatus().name(), w.getTotalAmount(),
            w.getCurrencyCode(), w.getIssuedAt(), w.getCompletedAt(), lineCount);
    }

    public static DeliveryLineDto toDto(DeliveryLine l) {
        return new DeliveryLineDto(l.getId(), l.getLineNo(), l.getItemCode(),
            l.getOrderedQuantity(), l.getReceivedQuantity(), l.outstandingQuantity(),
            l.getUnitCode(), l.getDueDate(), l.getStatus().name(), l.isOverdue());
    }

    public static GoodsReceiptDto toDto(GoodsReceipt r) {
        return new GoodsReceiptDto(r.getId(), r.getDeliveryLine().getLineNo(), r.getQuantity(),
            r.getReceivedBy(), r.getReceivedAt(), r.isLate(), r.getRemarks());
    }
}
