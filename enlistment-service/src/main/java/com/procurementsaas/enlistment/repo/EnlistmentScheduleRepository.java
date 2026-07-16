package com.procurementsaas.enlistment.repo;

import com.procurementsaas.enlistment.domain.EnlistmentSchedule;
import com.procurementsaas.enlistment.domain.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnlistmentScheduleRepository extends JpaRepository<EnlistmentSchedule, Long> {
    Optional<EnlistmentSchedule> findByCode(String code);
    boolean existsByCode(String code);
    List<EnlistmentSchedule> findByStatus(ScheduleStatus status);
}
