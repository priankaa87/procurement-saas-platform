package com.procurementsaas.contract.repo;

import com.procurementsaas.contract.domain.Award;
import com.procurementsaas.contract.domain.AwardStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AwardRepository extends JpaRepository<Award, Long> {
    Optional<Award> findByTenderCode(String tenderCode);
    boolean existsByTenderCode(String tenderCode);
    List<Award> findByStatus(AwardStatus status);

    /** Awards still unanswered after their deadline — candidates for expiry. */
    List<Award> findByStatusAndRespondByBefore(AwardStatus status, LocalDate date);
}
