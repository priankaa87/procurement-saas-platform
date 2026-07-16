package com.procurementsaas.reporting.repo;

import com.procurementsaas.reporting.domain.ReportJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportJobRepository extends JpaRepository<ReportJob, Long> {
    List<ReportJob> findByRequestedByOrderByCreatedAtDesc(String requestedBy);
    List<ReportJob> findTop50ByOrderByCreatedAtDesc();
}
