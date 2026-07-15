package com.procurementsaas.tenantbilling.repo;

import com.procurementsaas.tenantbilling.domain.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    Optional<Plan> findByCode(String code);
}
