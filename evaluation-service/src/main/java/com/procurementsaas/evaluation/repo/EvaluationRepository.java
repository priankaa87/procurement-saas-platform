package com.procurementsaas.evaluation.repo;

import com.procurementsaas.evaluation.domain.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    Optional<Evaluation> findByTenderCode(String tenderCode);
    boolean existsByTenderCode(String tenderCode);
}
