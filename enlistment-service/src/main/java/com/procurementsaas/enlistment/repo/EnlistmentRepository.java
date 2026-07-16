package com.procurementsaas.enlistment.repo;

import com.procurementsaas.enlistment.domain.Enlistment;
import com.procurementsaas.enlistment.domain.EnlistmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnlistmentRepository extends JpaRepository<Enlistment, Long> {
    List<Enlistment> findBySupplierCode(String supplierCode);
    List<Enlistment> findByCategoryCode(String categoryCode);

    /** Live records for a supplier — used when a debarment must withdraw them all. */
    List<Enlistment> findBySupplierCodeAndStatus(String supplierCode, EnlistmentStatus status);
}
