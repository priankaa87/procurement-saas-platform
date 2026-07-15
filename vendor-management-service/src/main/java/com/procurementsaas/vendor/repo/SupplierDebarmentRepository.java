package com.procurementsaas.vendor.repo;

import com.procurementsaas.vendor.domain.SupplierDebarment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierDebarmentRepository extends JpaRepository<SupplierDebarment, Long> {
    List<SupplierDebarment> findBySupplierId(Long supplierId);
    Optional<SupplierDebarment> findBySupplierIdAndActiveTrue(Long supplierId);
}
