package com.procurementsaas.tenantbilling.repo;

import com.procurementsaas.tenantbilling.domain.Tenant;
import com.procurementsaas.tenantbilling.domain.TenantStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findByTenantKey(String tenantKey);
    boolean existsByTenantKey(String tenantKey);
    List<Tenant> findByStatus(TenantStatus status);
}
