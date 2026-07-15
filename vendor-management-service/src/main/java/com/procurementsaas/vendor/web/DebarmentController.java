package com.procurementsaas.vendor.web;

import com.procurementsaas.vendor.dto.Dtos.DebarRequest;
import com.procurementsaas.vendor.dto.Dtos.DebarmentDto;
import com.procurementsaas.vendor.service.DebarmentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The debarment process. Deliberately guarded by its own feature — debarring a supplier
 * is a higher-privilege action than routine vendor administration.
 */
@RestController
@RequestMapping("/suppliers/{id}")
public class DebarmentController {

    private final DebarmentService debarmentService;

    public DebarmentController(DebarmentService debarmentService) {
        this.debarmentService = debarmentService;
    }

    @GetMapping("/debarments")
    @PreAuthorize("hasAuthority('FEATURE_VENDOR_VIEW')")
    public List<DebarmentDto> history(@PathVariable Long id) {
        return debarmentService.history(id);
    }

    @PostMapping("/debar")
    @PreAuthorize("hasAuthority('FEATURE_VENDOR_DEBAR')")
    public DebarmentDto debar(@PathVariable Long id, @Valid @RequestBody DebarRequest request) {
        return debarmentService.debar(id, request);
    }

    @PostMapping("/reinstate")
    @PreAuthorize("hasAuthority('FEATURE_VENDOR_DEBAR')")
    public DebarmentDto reinstate(@PathVariable Long id) {
        return debarmentService.reinstate(id);
    }
}
