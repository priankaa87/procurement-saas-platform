package com.procurementsaas.contract.web;

import com.procurementsaas.contract.dto.Dtos.AwardDto;
import com.procurementsaas.contract.dto.Dtos.DeclineRequest;
import com.procurementsaas.contract.dto.Dtos.IssueAwardRequest;
import com.procurementsaas.contract.service.AwardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/awards")
public class AwardController {

    private final AwardService awardService;

    public AwardController(AwardService awardService) {
        this.awardService = awardService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('FEATURE_AWARD_VIEW')")
    public List<AwardDto> list(@RequestParam(required = false) String status) {
        return awardService.list(status);
    }

    @GetMapping("/{tenderCode}")
    @PreAuthorize("hasAuthority('FEATURE_AWARD_VIEW')")
    public AwardDto get(@PathVariable String tenderCode) {
        return awardService.getByTender(tenderCode);
    }

    /** Manual issuance; awards normally arrive from the tender-awarded event. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FEATURE_AWARD_MANAGE')")
    public AwardDto issue(@Valid @RequestBody IssueAwardRequest request) {
        return awardService.issue(request);
    }

    /** The supplier's answer — a separate privilege from issuing the award. */
    @PostMapping("/{tenderCode}/accept")
    @PreAuthorize("hasAuthority('FEATURE_AWARD_RESPOND')")
    public AwardDto accept(@PathVariable String tenderCode) {
        return awardService.accept(tenderCode);
    }

    @PostMapping("/{tenderCode}/decline")
    @PreAuthorize("hasAuthority('FEATURE_AWARD_RESPOND')")
    public AwardDto decline(@PathVariable String tenderCode,
                            @Valid @RequestBody DeclineRequest request) {
        return awardService.decline(tenderCode, request);
    }

    @PostMapping("/{tenderCode}/cancel")
    @PreAuthorize("hasAuthority('FEATURE_AWARD_MANAGE')")
    public AwardDto cancel(@PathVariable String tenderCode) {
        return awardService.cancel(tenderCode);
    }

    /** Lapses every award whose acceptance window has closed. */
    @PostMapping("/expire-lapsed")
    @PreAuthorize("hasAuthority('FEATURE_AWARD_MANAGE')")
    public Map<String, Object> expireLapsed() {
        return Map.of("expired", awardService.expireLapsedAwards());
    }
}
