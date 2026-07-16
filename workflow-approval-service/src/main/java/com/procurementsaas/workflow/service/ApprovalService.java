package com.procurementsaas.workflow.service;

import com.procurementsaas.common.web.NotFoundException;
import com.procurementsaas.workflow.domain.ApprovalAction;
import com.procurementsaas.workflow.domain.ApprovalDecision;
import com.procurementsaas.workflow.domain.ApprovalRequest;
import com.procurementsaas.workflow.domain.ApprovalStatus;
import com.procurementsaas.workflow.domain.ApprovalStep;
import com.procurementsaas.workflow.domain.ApprovalWorkflow;
import com.procurementsaas.workflow.dto.Dtos.ActionDto;
import com.procurementsaas.workflow.dto.Dtos.DecideRequest;
import com.procurementsaas.workflow.dto.Dtos.RequestDto;
import com.procurementsaas.workflow.dto.Dtos.StartRequest;
import com.procurementsaas.workflow.repo.ApprovalActionRepository;
import com.procurementsaas.workflow.repo.ApprovalRequestRepository;
import com.procurementsaas.workflow.repo.ApprovalStepRepository;
import com.procurementsaas.workflow.repo.ApprovalWorkflowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Running approval requests: starting them, and taking decisions on their steps.
 *
 * <p>Everything interesting is in {@link #decide}, which has to answer two questions before
 * it will record anything: <em>is this person allowed to act on this step</em>, and
 * <em>should they be allowed to act on this particular request</em>. Those are different
 * questions, and conflating them is how self-approval slips through.
 */
@Service
@Transactional
public class ApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalService.class);

    private final ApprovalWorkflowRepository workflowRepository;
    private final ApprovalStepRepository stepRepository;
    private final ApprovalRequestRepository requestRepository;
    private final ApprovalActionRepository actionRepository;
    private final DelegationService delegationService;

    public ApprovalService(ApprovalWorkflowRepository workflowRepository,
                           ApprovalStepRepository stepRepository,
                           ApprovalRequestRepository requestRepository,
                           ApprovalActionRepository actionRepository,
                           DelegationService delegationService) {
        this.workflowRepository = workflowRepository;
        this.stepRepository = stepRepository;
        this.requestRepository = requestRepository;
        this.actionRepository = actionRepository;
        this.delegationService = delegationService;
    }

    /**
     * Raises a request against a workflow.
     *
     * <p>Refuses if the same subject already has one open — two live requests for the same
     * thing invite two contradictory answers.
     */
    public RequestDto start(String requestedBy, StartRequest request) {
        ApprovalWorkflow workflow = workflowRepository.findByCode(request.workflowCode())
            .orElseThrow(() -> new NotFoundException("Workflow not found: " + request.workflowCode()));
        if (!workflow.isActive()) {
            throw new IllegalStateException("Workflow is not active: " + workflow.getCode());
        }
        requestRepository.findBySubjectTypeAndSubjectRefAndStatus(
                workflow.getSubjectType(), request.subjectRef(), ApprovalStatus.PENDING)
            .ifPresent(open -> {
                throw new IllegalArgumentException("An approval is already open for "
                    + workflow.getSubjectType() + " " + request.subjectRef());
            });

        long steps = stepRepository.countByWorkflowId(workflow.getId());
        ApprovalRequest approvalRequest = new ApprovalRequest(workflow.getCode(),
            workflow.getSubjectType(), request.subjectRef(), requestedBy, request.reason(),
            (int) steps);
        return toDto(requestRepository.save(approvalRequest));
    }

    /**
     * Records a decision on the request's current step.
     *
     * @param actorId    the authenticated caller
     * @param actorRoles roles the caller holds directly, from their token
     */
    public RequestDto decide(Long requestId, String actorId, Set<String> actorRoles,
                             ApprovalDecision decision, DecideRequest body) {
        ApprovalRequest request = findRequest(requestId);
        if (!request.isOpen()) {
            throw new IllegalStateException(
                "Approval request " + requestId + " is already " + request.getStatus());
        }

        // Separation of duties. Checked before authority, because holding the right role is
        // exactly what makes self-approval tempting and plausible.
        if (request.wasRaisedBy(actorId)) {
            throw new IllegalStateException(
                "You cannot decide your own request (" + requestId + ")");
        }

        ApprovalStep step = currentStep(request);
        Authority authority = resolveAuthority(actorId, actorRoles, step.getRoleCode());

        actionRepository.save(new ApprovalAction(request, step.getStepNo(), step.getRoleCode(),
            actorId, authority.onBehalfOf(), decision, body == null ? null : body.comment()));

        if (decision == ApprovalDecision.REJECT) {
            request.reject();
            log.info("Request {} rejected at step {} by {}", requestId, step.getStepNo(), actorId);
        } else {
            boolean completed = request.approveCurrentStep();
            log.info("Request {} approved at step {} by {}{}", requestId, step.getStepNo(),
                actorId, completed ? " (final)" : "");
        }
        return toDto(requestRepository.save(request));
    }

    /** The requester may withdraw their own request while it is still open. */
    public RequestDto cancel(Long requestId, String actorId) {
        ApprovalRequest request = findRequest(requestId);
        if (!request.wasRaisedBy(actorId)) {
            throw new IllegalStateException(
                "Only " + request.getRequestedBy() + " can withdraw this request");
        }
        request.cancel();
        return toDto(requestRepository.save(request));
    }

    @Transactional(readOnly = true)
    public RequestDto get(Long requestId) {
        return toDto(findRequest(requestId));
    }

    @Transactional(readOnly = true)
    public List<RequestDto> pending() {
        return requestRepository.findByStatusOrderByCreatedAt(ApprovalStatus.PENDING).stream()
            .map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<RequestDto> forSubject(String subjectType, String subjectRef) {
        return requestRepository
            .findBySubjectTypeAndSubjectRefOrderByCreatedAtDesc(subjectType, subjectRef).stream()
            .map(this::toDto).toList();
    }

    /** The audit trail: who decided what, on whose authority. */
    @Transactional(readOnly = true)
    public List<ActionDto> history(Long requestId) {
        findRequest(requestId);
        return actionRepository.findByRequestIdOrderByActedAt(requestId).stream()
            .map(WorkflowMapper::toDto).toList();
    }

    /**
     * Decides whether an actor may act for a role: either they hold it, or someone who
     * does has lent it to them for a period that covers today.
     */
    private Authority resolveAuthority(String actorId, Set<String> actorRoles, String roleCode) {
        if (actorRoles.contains(roleCode)) {
            return Authority.ownRole(roleCode);
        }
        return delegationService.activeDelegationFor(actorId, roleCode)
            .map(d -> Authority.delegatedFrom(roleCode, d.getFromUser()))
            .orElseThrow(() -> new AccessDeniedForStepException(
                "This step requires the " + roleCode + " role, held directly or by delegation"));
    }

    private ApprovalStep currentStep(ApprovalRequest request) {
        ApprovalWorkflow workflow = workflowRepository.findByCode(request.getWorkflowCode())
            .orElseThrow(() -> new NotFoundException(
                "Workflow not found: " + request.getWorkflowCode()));
        return stepRepository.findByWorkflowIdAndStepNo(workflow.getId(), request.getCurrentStep())
            .orElseThrow(() -> new NotFoundException(
                "Step " + request.getCurrentStep() + " not found on " + workflow.getCode()));
    }

    private ApprovalRequest findRequest(Long id) {
        return requestRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Approval request not found: " + id));
    }

    private RequestDto toDto(ApprovalRequest request) {
        String roleCode = null;
        if (request.isOpen()) {
            roleCode = currentStep(request).getRoleCode();
        }
        return WorkflowMapper.toDto(request, roleCode);
    }

    /** Raised when the caller lacks the role a step demands; surfaced as 403. */
    @org.springframework.web.bind.annotation.ResponseStatus(
        org.springframework.http.HttpStatus.FORBIDDEN)
    public static class AccessDeniedForStepException extends RuntimeException {
        public AccessDeniedForStepException(String message) {
            super(message);
        }
    }
}
