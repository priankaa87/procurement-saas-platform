package com.procurementsaas.enlistment.service;

import com.procurementsaas.enlistment.domain.Enlistment;
import com.procurementsaas.enlistment.domain.EnlistmentStatus;
import com.procurementsaas.enlistment.dto.Dtos.EnlistmentDto;
import com.procurementsaas.enlistment.repo.EnlistmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** The register of who is currently qualified for what. */
@Service
@Transactional
public class EnlistmentService {

    private static final Logger log = LoggerFactory.getLogger(EnlistmentService.class);

    private final EnlistmentRepository enlistmentRepository;

    public EnlistmentService(EnlistmentRepository enlistmentRepository) {
        this.enlistmentRepository = enlistmentRepository;
    }

    @Transactional(readOnly = true)
    public List<EnlistmentDto> forSupplier(String supplierCode) {
        return enlistmentRepository.findBySupplierCode(supplierCode).stream()
            .map(EnlistmentMapper::toDto).toList();
    }

    /**
     * Who may currently bid in a category.
     *
     * <p>Filtered on live validity rather than on the stored status, so an enlistment that
     * ran out yesterday is absent today without anything having had to expire it.
     */
    @Transactional(readOnly = true)
    public List<EnlistmentDto> qualifiedFor(String categoryCode) {
        return enlistmentRepository.findByCategoryCode(categoryCode).stream()
            .filter(Enlistment::isCurrentlyValid)
            .map(EnlistmentMapper::toDto).toList();
    }

    /** Whether a supplier may bid in a category right now. */
    @Transactional(readOnly = true)
    public boolean isQualified(String supplierCode, String categoryCode) {
        return enlistmentRepository.findBySupplierCode(supplierCode).stream()
            .filter(e -> e.getCategoryCode().equals(categoryCode))
            .anyMatch(Enlistment::isCurrentlyValid);
    }

    /**
     * Withdraws every live enlistment a supplier holds.
     *
     * <p>Called when a supplier is debarred. A debarred supplier who kept their
     * pre-qualifications would still show up as eligible to bid, which is precisely the
     * thing debarment exists to prevent.
     *
     * @return how many were withdrawn
     */
    public int revokeAllFor(String supplierCode, String reason) {
        List<Enlistment> active = enlistmentRepository
            .findBySupplierCodeAndStatus(supplierCode, EnlistmentStatus.ACTIVE);
        active.forEach(enlistment -> {
            enlistment.revoke(reason);
            enlistmentRepository.save(enlistment);
        });
        if (!active.isEmpty()) {
            log.info("Revoked {} enlistment(s) for {}: {}", active.size(), supplierCode, reason);
        }
        return active.size();
    }
}
