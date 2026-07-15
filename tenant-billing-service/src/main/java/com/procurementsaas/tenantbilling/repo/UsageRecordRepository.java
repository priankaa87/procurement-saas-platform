package com.procurementsaas.tenantbilling.repo;

import com.procurementsaas.tenantbilling.domain.UsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsageRecordRepository extends JpaRepository<UsageRecord, Long> {

    /** Running total for a metric, summed from the ledger. Zero when nothing recorded. */
    @Query("""
        SELECT COALESCE(SUM(u.quantity), 0) FROM UsageRecord u
        WHERE u.tenant.id = :tenantId AND u.metric = :metric
        """)
    long totalFor(@Param("tenantId") Long tenantId, @Param("metric") String metric);
}
