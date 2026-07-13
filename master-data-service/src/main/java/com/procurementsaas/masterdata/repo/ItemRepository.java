package com.procurementsaas.masterdata.repo;

import com.procurementsaas.masterdata.domain.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {
    Optional<Item> findByCode(String code);
    boolean existsByCode(String code);
}
