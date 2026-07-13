package com.procurementsaas.masterdata.repo;

import com.procurementsaas.masterdata.domain.MeasurementUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MeasurementUnitRepository extends JpaRepository<MeasurementUnit, Long> {
    Optional<MeasurementUnit> findByCode(String code);
    boolean existsByCode(String code);
}
