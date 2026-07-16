package com.procurementsaas.contract.repo;

import com.procurementsaas.contract.domain.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    Optional<WorkOrder> findByCode(String code);
    boolean existsByCode(String code);
    List<WorkOrder> findByAwardId(Long awardId);
}
