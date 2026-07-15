package com.procurementsaas.tender.repo;

import com.procurementsaas.tender.domain.Tender;
import com.procurementsaas.tender.domain.TenderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenderRepository extends JpaRepository<Tender, Long> {
    Optional<Tender> findByCode(String code);
    boolean existsByCode(String code);
    List<Tender> findByStatus(TenderStatus status);
}
