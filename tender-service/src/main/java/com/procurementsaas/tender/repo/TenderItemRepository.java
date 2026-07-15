package com.procurementsaas.tender.repo;

import com.procurementsaas.tender.domain.TenderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TenderItemRepository extends JpaRepository<TenderItem, Long> {
    List<TenderItem> findByTenderId(Long tenderId);
    long countByTenderId(Long tenderId);
}
