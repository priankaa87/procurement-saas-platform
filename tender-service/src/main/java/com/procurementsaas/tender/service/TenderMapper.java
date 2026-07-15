package com.procurementsaas.tender.service;

import com.procurementsaas.tender.domain.Bid;
import com.procurementsaas.tender.domain.Tender;
import com.procurementsaas.tender.domain.TenderItem;
import com.procurementsaas.tender.domain.TenderParticipant;
import com.procurementsaas.tender.dto.Dtos.BidDto;
import com.procurementsaas.tender.dto.Dtos.ParticipantDto;
import com.procurementsaas.tender.dto.Dtos.TenderDto;
import com.procurementsaas.tender.dto.Dtos.TenderItemDto;

/** Maps tender entities to API DTOs. */
public final class TenderMapper {

    private TenderMapper() {
    }

    public static TenderDto toDto(Tender t, long itemCount) {
        return new TenderDto(t.getId(), t.getCode(), t.getTitle(), t.getDescription(),
            t.getStatus().name(), t.getCurrencyCode(), t.getBidDeadline(), t.getPublishedAt(),
            t.getOpenedAt(), t.getAwardedSupplierCode(), itemCount);
    }

    public static TenderItemDto toDto(TenderItem i) {
        return new TenderItemDto(i.getId(), i.getItemCode(), i.getDescription(), i.getQuantity(),
            i.getUnitCode());
    }

    public static ParticipantDto toDto(TenderParticipant p) {
        return new ParticipantDto(p.getId(), p.getSupplierCode(), p.getStatus().name(),
            p.getInvitedAt());
    }

    public static BidDto toDto(Bid b) {
        return new BidDto(b.getId(), b.getSupplierCode(), b.getTotalAmount(), b.getCurrencyCode(),
            b.getNotes(), b.getSubmittedAt());
    }
}
