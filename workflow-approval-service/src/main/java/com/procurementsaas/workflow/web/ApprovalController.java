package com.procurementsaas.workflow.web;

import com.procurementsaas.workflow.domain.ApprovalDecision;
import com.procurementsaas.workflow.dto.Dtos.ActionDto;
import com.procurementsaas.workflow.dto.Dtos.DecideRequest;
import com.procurementsaas.workflow.dto.Dtos.RequestDto;
import com.procurementsaas.workflow.dto.Dtos.StartRequest;
import com.procurementsaas.workflow.service.ApprovalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/approvals")
public class ApprovalController {

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('FEATURE_APPROVAL_VIEW')")
    public List<RequestDto> pending() {
        return approvalService.pending();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FEATURE_APPROVAL_VIEW')")
    public RequestDto get(@PathVariable Long id) {
        return approvalService.get(id);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('FEATURE_APPROVAL_VIEW')")
    public List<RequestDto> forSubject(@RequestParam String subjectType,
                                       @RequestParam String subjectRef) {
        return approvalService.forSubject(subjectType, subjectRef);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FEATURE_APPROVAL_REQUEST')")
    public RequestDto start(@Valid @RequestBody StartRequest request,
                            Authentication authentication) {
        return approvalService.start(CallerRoles.actorId(authentication), request);
    }

    /**
     * Approves the current step.
     *
     * <p>The endpoint-level privilege only says "this person may take part in approvals at
     * all". Whether they may decide <em>this</em> step of <em>this</em> request depends on
     * the step's role, any delegation, and who raised it — all settled in the service.
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('FEATURE_APPROVAL_ACT')")
    public RequestDto approve(@PathVariable Long id,
                              @RequestBody(required = false) DecideRequest body,
                              Authentication authentication) {
        return approvalService.decide(id, CallerRoles.actorId(authentication),
            CallerRoles.of(authentication), ApprovalDecision.APPROVE, body);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('FEATURE_APPROVAL_ACT')")
    public RequestDto reject(@PathVariable Long id,
                             @RequestBody(required = false) DecideRequest body,
                             Authentication authentication) {
        return approvalService.decide(id, CallerRoles.actorId(authentication),
            CallerRoles.of(authentication), ApprovalDecision.REJECT, body);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('FEATURE_APPROVAL_REQUEST')")
    public RequestDto cancel(@PathVariable Long id, Authentication authentication) {
        return approvalService.cancel(id, CallerRoles.actorId(authentication));
    }

    /** Who decided what, and on whose authority. */
    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('FEATURE_APPROVAL_VIEW')")
    public List<ActionDto> history(@PathVariable Long id) {
        return approvalService.history(id);
    }
}
