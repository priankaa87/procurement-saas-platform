package com.procurementsaas.tenantbilling.repo;

import com.procurementsaas.tenantbilling.domain.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByTenantIdOrderByPeriodStartDesc(Long tenantId);
    boolean existsByTenantIdAndPeriodStart(Long tenantId, LocalDate periodStart);
}
