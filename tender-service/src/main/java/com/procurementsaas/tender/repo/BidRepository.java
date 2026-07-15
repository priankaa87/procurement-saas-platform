package com.procurementsaas.tender.repo;

import com.procurementsaas.tender.domain.Bid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BidRepository extends JpaRepository<Bid, Long> {
    List<Bid> findByTenderIdOrderByTotalAmountAsc(Long tenderId);
    Optional<Bid> findByTenderIdAndSupplierCode(Long tenderId, String supplierCode);
    boolean existsByTenderIdAndSupplierCode(Long tenderId, String supplierCode);
    long countByTenderId(Long tenderId);
}
