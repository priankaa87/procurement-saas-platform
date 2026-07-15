package com.procurementsaas.tender.service;

import com.procurementsaas.common.web.NotFoundException;
import com.procurementsaas.tender.domain.Bid;
import com.procurementsaas.tender.domain.Tender;
import com.procurementsaas.tender.domain.TenderParticipant;
import com.procurementsaas.tender.dto.Dtos.BidDto;
import com.procurementsaas.tender.dto.Dtos.BidReceiptDto;
import com.procurementsaas.tender.dto.Dtos.SubmitBidRequest;
import com.procurementsaas.tender.repo.BidRepository;
import com.procurementsaas.tender.repo.TenderParticipantRepository;
import com.procurementsaas.tender.repo.TenderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Bid submission and disclosure.
 *
 * <p>This class exists to protect one rule: <strong>bids stay sealed until the tender is
 * opened</strong>. Submission returns only a receipt (never an amount), and disclosure is
 * refused outright while the tender is sealed — so no caller, however privileged, can read
 * bids early through this service.
 */
@Service
@Transactional
public class BidService {

    private final TenderRepository tenderRepository;
    private final TenderParticipantRepository participantRepository;
    private final BidRepository bidRepository;

    public BidService(TenderRepository tenderRepository,
                      TenderParticipantRepository participantRepository,
                      BidRepository bidRepository) {
        this.tenderRepository = tenderRepository;
        this.participantRepository = participantRepository;
        this.bidRepository = bidRepository;
    }

    /**
     * Submits a sealed bid. Only invited suppliers may bid, only while the tender is
     * accepting bids, and only once.
     */
    public BidReceiptDto submit(Long tenderId, SubmitBidRequest request) {
        Tender tender = findTender(tenderId);

        if (!tender.acceptingBids()) {
            throw new IllegalStateException(
                "Tender is not accepting bids (status " + tender.getStatus()
                    + ", deadline " + tender.getBidDeadline() + "): " + tender.getCode());
        }
        TenderParticipant participant = participantRepository
            .findByTenderIdAndSupplierCode(tenderId, request.supplierCode())
            .orElseThrow(() -> new IllegalArgumentException(
                "Supplier was not invited to this tender: " + request.supplierCode()));

        if (bidRepository.existsByTenderIdAndSupplierCode(tenderId, request.supplierCode())) {
            throw new IllegalArgumentException(
                "A bid has already been submitted by: " + request.supplierCode());
        }

        Bid bid = new Bid(tender, request.supplierCode(), request.totalAmount(),
            tender.getCurrencyCode(), request.notes());
        Bid saved = bidRepository.save(bid);
        participant.markBidSubmitted();
        participantRepository.save(participant);

        return new BidReceiptDto(saved.getId(), saved.getSupplierCode(), saved.getSubmittedAt(),
            "Bid received and sealed until the tender is opened");
    }

    /**
     * Discloses all bids, cheapest first — only once the tender has been opened.
     *
     * @throws IllegalStateException while the tender is still sealed (surfaced as 409)
     */
    @Transactional(readOnly = true)
    public List<BidDto> disclose(Long tenderId) {
        Tender tender = findTender(tenderId);
        if (tender.bidsAreSealed()) {
            throw new IllegalStateException(
                "Bids are sealed until the tender is opened: " + tender.getCode());
        }
        return bidRepository.findByTenderIdOrderByTotalAmountAsc(tenderId).stream()
            .map(TenderMapper::toDto).toList();
    }

    /** How many bids have been received. Safe to expose while sealed — a count, not content. */
    @Transactional(readOnly = true)
    public long count(Long tenderId) {
        findTender(tenderId);
        return bidRepository.countByTenderId(tenderId);
    }

    private Tender findTender(Long id) {
        return tenderRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Tender not found: " + id));
    }
}
