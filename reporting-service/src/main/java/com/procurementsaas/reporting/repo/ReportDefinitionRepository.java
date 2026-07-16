package com.procurementsaas.reporting.repo;

import com.procurementsaas.reporting.domain.ReportDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportDefinitionRepository extends JpaRepository<ReportDefinition, Long> {
    Optional<ReportDefinition> findByCode(String code);
}
