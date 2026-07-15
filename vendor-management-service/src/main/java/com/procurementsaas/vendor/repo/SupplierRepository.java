package com.procurementsaas.vendor.repo;

import com.procurementsaas.vendor.domain.Supplier;
import com.procurementsaas.vendor.domain.SupplierStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    Optional<Supplier> findByCode(String code);
    boolean existsByCode(String code);
    List<Supplier> findByStatus(SupplierStatus status);
    List<Supplier> findByCategoryCodesContaining(String categoryCode);
}
