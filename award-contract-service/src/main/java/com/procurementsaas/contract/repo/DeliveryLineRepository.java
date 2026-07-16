package com.procurementsaas.contract.repo;

import com.procurementsaas.contract.domain.DeliveryLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryLineRepository extends JpaRepository<DeliveryLine, Long> {
    List<DeliveryLine> findByWorkOrderIdOrderByLineNo(Long workOrderId);
    Optional<DeliveryLine> findByWorkOrderIdAndLineNo(Long workOrderId, int lineNo);
    long countByWorkOrderId(Long workOrderId);
}
