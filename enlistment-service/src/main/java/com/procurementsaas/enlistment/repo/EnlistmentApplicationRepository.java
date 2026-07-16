package com.procurementsaas.enlistment.repo;

import com.procurementsaas.enlistment.domain.ApplicationStatus;
import com.procurementsaas.enlistment.domain.EnlistmentApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnlistmentApplicationRepository extends JpaRepository<EnlistmentApplication, Long> {
    List<EnlistmentApplication> findByScheduleId(Long scheduleId);
    Optional<EnlistmentApplication> findByScheduleIdAndSupplierCode(Long scheduleId, String supplierCode);
    boolean existsByScheduleIdAndSupplierCode(Long scheduleId, String supplierCode);
    long countByScheduleIdAndStatus(Long scheduleId, ApplicationStatus status);
}
