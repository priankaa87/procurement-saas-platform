package com.procurementsaas.workflow.service;

import com.procurementsaas.common.web.NotFoundException;
import com.procurementsaas.workflow.domain.Delegation;
import com.procurementsaas.workflow.dto.Dtos.CreateDelegationRequest;
import com.procurementsaas.workflow.dto.Dtos.DelegationDto;
import com.procurementsaas.workflow.repo.DelegationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Granting, listing, and revoking delegated authority. */
@Service
@Transactional
public class DelegationService {

    private final DelegationRepository delegationRepository;

    public DelegationService(DelegationRepository delegationRepository) {
        this.delegationRepository = delegationRepository;
    }

    /**
     * Creates a delegation from the caller to someone else.
     *
     * <p>{@code fromUser} is taken from the authenticated caller, never from the request
     * body: letting a client name the delegator would let anyone hand themselves someone
     * else's authority.
     *
     * <p>You may only lend authority you actually hold. Without this, delegation becomes a
     * way to conjure a role out of nothing — grant yourself the role you want by naming it
     * in a delegation to a colleague, who then delegates it back.
     */
    public DelegationDto create(String fromUser, Set<String> fromUserRoles,
                                CreateDelegationRequest request) {
        if (!fromUserRoles.contains(request.roleCode())) {
            throw new IllegalStateException(
                "You cannot delegate the " + request.roleCode() + " role because you do not hold it");
        }
        Delegation delegation = new Delegation(fromUser, request.toUser(), request.roleCode(),
            request.validFrom(), request.validTo(), request.reason());
        return WorkflowMapper.toDto(delegationRepository.save(delegation));
    }

    @Transactional(readOnly = true)
    public List<DelegationDto> granted(String fromUser) {
        return delegationRepository.findByFromUserOrderByValidFromDesc(fromUser).stream()
            .map(WorkflowMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<DelegationDto> received(String toUser) {
        return delegationRepository.findByToUserOrderByValidFromDesc(toUser).stream()
            .map(WorkflowMapper::toDto).toList();
    }

    /** Only the person who lent the authority may take it back. */
    public DelegationDto revoke(Long id, String actorId) {
        Delegation delegation = delegationRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Delegation not found: " + id));
        if (!delegation.getFromUser().equals(actorId)) {
            throw new IllegalStateException(
                "Only " + delegation.getFromUser() + " can revoke this delegation");
        }
        delegation.revoke();
        return WorkflowMapper.toDto(delegationRepository.save(delegation));
    }

    /**
     * Finds a delegation letting {@code actorId} exercise {@code roleCode} today.
     *
     * <p>Validity is re-checked against today's date on every use rather than trusted from
     * a flag — a delegation that has run out must stop working on its own, without anyone
     * having to remember to switch it off.
     */
    @Transactional(readOnly = true)
    public Optional<Delegation> activeDelegationFor(String actorId, String roleCode) {
        LocalDate today = LocalDate.now();
        return delegationRepository.findByToUserAndRoleCodeAndRevokedFalse(actorId, roleCode)
            .stream()
            .filter(d -> d.isValidOn(today))
            .findFirst();
    }
}
