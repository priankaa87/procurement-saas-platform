package com.procurementsaas.tender.web;

import com.procurementsaas.tender.dto.Dtos.BidDto;
import com.procurementsaas.tender.dto.Dtos.BidReceiptDto;
import com.procurementsaas.tender.dto.Dtos.SubmitBidRequest;
import com.procurementsaas.tender.service.BidService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Bid submission and disclosure.
 *
 * <p>Submitting requires {@code FEATURE_TENDER_BID} (a supplier-side privilege); reading
 * bids requires {@code FEATURE_TENDER_EVALUATE} — and even then only after opening.
 */
@RestController
@RequestMapping("/tenders/{id}/bids")
public class BidController {

    private final BidService bidService;

    public BidController(BidService bidService) {
        this.bidService = bidService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FEATURE_TENDER_BID')")
    public BidReceiptDto submit(@PathVariable Long id, @Valid @RequestBody SubmitBidRequest request) {
        return bidService.submit(id, request);
    }

    /** Refused with 409 while the tender is still sealed. */
    @GetMapping
    @PreAuthorize("hasAuthority('FEATURE_TENDER_EVALUATE')")
    public List<BidDto> disclose(@PathVariable Long id) {
        return bidService.disclose(id);
    }

    /** A count reveals participation without revealing any bid content. */
    @GetMapping("/count")
    @PreAuthorize("hasAuthority('FEATURE_TENDER_VIEW')")
    public Map<String, Object> count(@PathVariable Long id) {
        return Map.of("tenderId", id, "bidCount", bidService.count(id));
    }
}
