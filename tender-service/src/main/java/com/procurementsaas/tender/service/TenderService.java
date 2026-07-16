package com.procurementsaas.tender.service;

import com.procurementsaas.common.tenancy.TenantContext;
import com.procurementsaas.common.web.NotFoundException;
import com.procurementsaas.events.TenderAwardedEvent;
import com.procurementsaas.events.TenderPublishedEvent;
import com.procurementsaas.tender.domain.Bid;
import com.procurementsaas.tender.domain.Tender;
import com.procurementsaas.tender.domain.TenderItem;
import com.procurementsaas.tender.domain.TenderParticipant;
import com.procurementsaas.tender.domain.TenderStatus;
import com.procurementsaas.tender.dto.Dtos.AddItemRequest;
import com.procurementsaas.tender.dto.Dtos.AwardRequest;
import com.procurementsaas.tender.dto.Dtos.CreateTenderRequest;
import com.procurementsaas.tender.dto.Dtos.InviteRequest;
import com.procurementsaas.tender.dto.Dtos.ParticipantDto;
import com.procurementsaas.tender.dto.Dtos.TenderDto;
import com.procurementsaas.tender.dto.Dtos.TenderItemDto;
import com.procurementsaas.tender.repo.BidRepository;
import com.procurementsaas.tender.repo.TenderItemRepository;
import com.procurementsaas.tender.repo.TenderParticipantRepository;
import com.procurementsaas.tender.repo.TenderRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** Drafting, publishing, opening, and awarding tenders. */
@Service
@Transactional
public class TenderService {

    private final TenderRepository tenderRepository;
    private final TenderItemRepository itemRepository;
    private final TenderParticipantRepository participantRepository;
    private final BidRepository bidRepository;
    private final ApplicationEventPublisher events;

    public TenderService(TenderRepository tenderRepository,
                         TenderItemRepository itemRepository,
                         TenderParticipantRepository participantRepository,
                         BidRepository bidRepository,
                         ApplicationEventPublisher events) {
        this.tenderRepository = tenderRepository;
        this.itemRepository = itemRepository;
        this.participantRepository = participantRepository;
        this.bidRepository = bidRepository;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<TenderDto> list(String status) {
        List<Tender> tenders = (status == null)
            ? tenderRepository.findAll()
            : tenderRepository.findByStatus(parseStatus(status));
        return tenders.stream().map(this::withItemCount).toList();
    }

    @Transactional(readOnly = true)
    public TenderDto get(Long id) {
        return withItemCount(findTender(id));
    }

    public TenderDto create(CreateTenderRequest request) {
        if (tenderRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Tender code already exists: " + request.code());
        }
        Tender tender = new Tender(request.code(), request.title(), request.description(),
            request.currencyCode(), request.bidDeadline());
        return withItemCount(tenderRepository.save(tender));
    }

    // --- Items ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<TenderItemDto> listItems(Long tenderId) {
        findTender(tenderId);
        return itemRepository.findByTenderId(tenderId).stream().map(TenderMapper::toDto).toList();
    }

    /** Items may only change while the tender is still a draft. */
    public TenderItemDto addItem(Long tenderId, AddItemRequest request) {
        Tender tender = findTender(tenderId);
        if (tender.getStatus() != TenderStatus.DRAFT) {
            throw new IllegalStateException(
                "Items can only be added while the tender is a DRAFT: " + tender.getCode());
        }
        TenderItem item = new TenderItem(tender, request.itemCode(), request.description(),
            request.quantity(), request.unitCode());
        return TenderMapper.toDto(itemRepository.save(item));
    }

    // --- Participants --------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ParticipantDto> listParticipants(Long tenderId) {
        findTender(tenderId);
        return participantRepository.findByTenderId(tenderId).stream()
            .map(TenderMapper::toDto).toList();
    }

    /** Invitations close once bidding is over. */
    public ParticipantDto invite(Long tenderId, InviteRequest request) {
        Tender tender = findTender(tenderId);
        if (tender.getStatus() != TenderStatus.DRAFT && tender.getStatus() != TenderStatus.PUBLISHED) {
            throw new IllegalStateException(
                "Suppliers can only be invited before the tender is opened: " + tender.getCode());
        }
        if (participantRepository.existsByTenderIdAndSupplierCode(tenderId, request.supplierCode())) {
            throw new IllegalArgumentException(
                "Supplier is already invited: " + request.supplierCode());
        }
        TenderParticipant participant = new TenderParticipant(tender, request.supplierCode());
        return TenderMapper.toDto(participantRepository.save(participant));
    }

    // --- Lifecycle -----------------------------------------------------------

    public TenderDto publish(Long id) {
        Tender tender = findTender(id);
        tender.publish(itemRepository.countByTenderId(id));
        Tender saved = tenderRepository.save(tender);

        List<String> invited = participantRepository.findByTenderId(id).stream()
            .map(TenderParticipant::getSupplierCode)
            .toList();
        // Handed to Kafka only once this transaction commits (see TenderEventForwarder).
        events.publishEvent(new TenderPublishedEvent(TenantContext.getTenant(), saved.getCode(),
            saved.getTitle(), saved.getBidDeadline(), invited, Instant.now()));

        return withItemCount(saved);
    }

    /** Opens the tender after the deadline, unsealing bids for evaluation. */
    public TenderDto open(Long id) {
        Tender tender = findTender(id);
        tender.open();
        return withItemCount(tenderRepository.save(tender));
    }

    /** Awards the tender; the winner must actually have bid. */
    public TenderDto award(Long id, AwardRequest request) {
        Tender tender = findTender(id);
        Bid winningBid = bidRepository.findByTenderIdAndSupplierCode(id, request.supplierCode())
            .orElseThrow(() -> new IllegalArgumentException(
                "Cannot award to a supplier that did not bid: " + request.supplierCode()));

        tender.award(request.supplierCode());
        Tender saved = tenderRepository.save(tender);

        // Losing bidders are named in the event so consumers needn't work out who lost.
        List<String> unsuccessful = bidRepository.findByTenderIdOrderByTotalAmountAsc(id).stream()
            .map(Bid::getSupplierCode)
            .filter(code -> !code.equals(request.supplierCode()))
            .toList();
        // The winning amount travels with the event so a contract can be raised from it
        // without reading back into this service, and is fixed as of the award.
        events.publishEvent(new TenderAwardedEvent(TenantContext.getTenant(), saved.getCode(),
            saved.getTitle(), request.supplierCode(), winningBid.getTotalAmount(),
            winningBid.getCurrencyCode(), unsuccessful, Instant.now()));

        return withItemCount(saved);
    }

    public TenderDto cancel(Long id) {
        Tender tender = findTender(id);
        tender.cancel();
        return withItemCount(tenderRepository.save(tender));
    }

    Tender findTender(Long id) {
        return tenderRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Tender not found: " + id));
    }

    private TenderDto withItemCount(Tender tender) {
        return TenderMapper.toDto(tender, itemRepository.countByTenderId(tender.getId()));
    }

    private static TenderStatus parseStatus(String status) {
        try {
            return TenderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown tender status: " + status);
        }
    }
}
