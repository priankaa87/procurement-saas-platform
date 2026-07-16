package com.procurementsaas.contract.repo;

import com.procurementsaas.contract.domain.GoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {
    List<GoodsReceipt> findByDeliveryLineIdOrderByReceivedAt(Long deliveryLineId);
}
