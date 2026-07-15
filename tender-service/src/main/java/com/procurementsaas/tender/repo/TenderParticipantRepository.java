package com.procurementsaas.tender.repo;

import com.procurementsaas.tender.domain.TenderParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenderParticipantRepository extends JpaRepository<TenderParticipant, Long> {
    List<TenderParticipant> findByTenderId(Long tenderId);
    Optional<TenderParticipant> findByTenderIdAndSupplierCode(Long tenderId, String supplierCode);
    boolean existsByTenderIdAndSupplierCode(Long tenderId, String supplierCode);
}
