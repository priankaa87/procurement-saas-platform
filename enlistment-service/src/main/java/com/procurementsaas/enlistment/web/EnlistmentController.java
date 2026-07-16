package com.procurementsaas.enlistment.web;

import com.procurementsaas.enlistment.dto.Dtos.EnlistmentDto;
import com.procurementsaas.enlistment.service.EnlistmentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * The register of who is qualified for what.
 *
 * <p>The Tender service reads this to decide who may be invited to bid.
 */
@RestController
@RequestMapping("/enlistments")
public class EnlistmentController {

    private final EnlistmentService enlistmentService;

    public EnlistmentController(EnlistmentService enlistmentService) {
        this.enlistmentService = enlistmentService;
    }

    /** Everything a supplier holds, current or not. */
    @GetMapping
    @PreAuthorize("hasAuthority('FEATURE_ENLISTMENT_VIEW')")
    public List<EnlistmentDto> forSupplier(@RequestParam String supplierCode) {
        return enlistmentService.forSupplier(supplierCode);
    }

    /** Only suppliers who may bid in this category today. */
    @GetMapping("/qualified")
    @PreAuthorize("hasAuthority('FEATURE_ENLISTMENT_VIEW')")
    public List<EnlistmentDto> qualifiedFor(@RequestParam String categoryCode) {
        return enlistmentService.qualifiedFor(categoryCode);
    }

    /** A direct yes/no, for services deciding whether to invite a supplier. */
    @GetMapping("/check")
    @PreAuthorize("hasAuthority('FEATURE_ENLISTMENT_VIEW')")
    public Map<String, Object> check(@RequestParam String supplierCode,
                                     @RequestParam String categoryCode) {
        return Map.of(
            "supplierCode", supplierCode,
            "categoryCode", categoryCode,
            "qualified", enlistmentService.isQualified(supplierCode, categoryCode));
    }
}
