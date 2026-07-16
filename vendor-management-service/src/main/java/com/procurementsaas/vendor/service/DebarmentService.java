package com.procurementsaas.vendor.service;

import com.procurementsaas.common.tenancy.TenantContext;
import com.procurementsaas.common.web.NotFoundException;
import com.procurementsaas.events.SupplierDebarredEvent;
import com.procurementsaas.vendor.domain.Supplier;
import com.procurementsaas.vendor.domain.SupplierDebarment;
import com.procurementsaas.vendor.domain.SupplierStatus;
import com.procurementsaas.vendor.dto.Dtos.DebarRequest;
import com.procurementsaas.vendor.dto.Dtos.DebarmentDto;
import com.procurementsaas.vendor.repo.SupplierDebarmentRepository;
import com.procurementsaas.vendor.repo.SupplierRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * The debarment process: blocking a supplier from participating, and reinstating it.
 *
 * <p>Debarment records are an audit trail — reinstatement closes the open record rather
 * than deleting it, so the history of why a supplier was blocked is never lost.
 */
@Service
@Transactional
public class DebarmentService {

    private final SupplierRepository supplierRepository;
    private final SupplierDebarmentRepository debarmentRepository;
    private final ApplicationEventPublisher events;

    public DebarmentService(SupplierRepository supplierRepository,
                            SupplierDebarmentRepository debarmentRepository,
                            ApplicationEventPublisher events) {
        this.supplierRepository = supplierRepository;
        this.debarmentRepository = debarmentRepository;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<DebarmentDto> history(Long supplierId) {
        findSupplier(supplierId);
        return debarmentRepository.findBySupplierId(supplierId).stream()
            .map(VendorMapper::toDto).toList();
    }

    /** Debars a supplier, recording the decision and blocking further participation. */
    public DebarmentDto debar(Long supplierId, DebarRequest request) {
        Supplier supplier = findSupplier(supplierId);
        if (supplier.getStatus() == SupplierStatus.DEBARRED) {
            throw new IllegalArgumentException("Supplier is already debarred: " + supplier.getCode());
        }
        if (request.debarredUntil() != null && request.debarredUntil().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Debarment end date cannot be in the past");
        }
        SupplierDebarment debarment = new SupplierDebarment(supplier, request.reason(),
            LocalDate.now(), request.debarredUntil());
        supplier.markDebarred();
        supplierRepository.save(supplier);
        SupplierDebarment saved = debarmentRepository.save(debarment);

        // Announced once committed. Other services withdraw the supplier's
        // pre-qualifications and tell them why; none of that is this service's concern,
        // and none of it may fail the debarment.
        events.publishEvent(new SupplierDebarredEvent(TenantContext.getTenant(),
            supplier.getCode(), supplier.getName(), request.reason(), request.debarredUntil(),
            Instant.now()));

        return VendorMapper.toDto(saved);
    }

    /** Lifts the active debarment and returns the supplier to active participation. */
    public DebarmentDto reinstate(Long supplierId) {
        Supplier supplier = findSupplier(supplierId);
        SupplierDebarment active = debarmentRepository.findBySupplierIdAndActiveTrue(supplierId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Supplier has no active debarment: " + supplier.getCode()));
        active.close();
        supplier.markReinstated();
        supplierRepository.save(supplier);
        return VendorMapper.toDto(debarmentRepository.save(active));
    }

    private Supplier findSupplier(Long id) {
        return supplierRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Supplier not found: " + id));
    }
}
