package com.procurementsaas.evaluation.service;

import com.procurementsaas.evaluation.domain.CriterionScore;
import com.procurementsaas.evaluation.domain.Evaluation;
import com.procurementsaas.evaluation.domain.EvaluationCriterion;
import com.procurementsaas.evaluation.domain.ParticipantEvaluation;
import com.procurementsaas.evaluation.dto.Dtos.CriterionDto;
import com.procurementsaas.evaluation.dto.Dtos.EvaluationDto;
import com.procurementsaas.evaluation.dto.Dtos.ParticipantDto;
import com.procurementsaas.evaluation.dto.Dtos.ScoreDto;

/** Maps evaluation entities to API DTOs. */
public final class EvaluationMapper {

    private EvaluationMapper() {
    }

    public static EvaluationDto toDto(Evaluation e) {
        return new EvaluationDto(e.getId(), e.getTenderCode(), e.getStatus().name(),
            e.getTechnicalWeight(), e.getFinancialWeight(), e.getPassMark());
    }

    public static CriterionDto toDto(EvaluationCriterion c) {
        return new CriterionDto(c.getId(), c.getCode(), c.getName(), c.getWeight());
    }

    public static ParticipantDto toDto(ParticipantEvaluation p) {
        return new ParticipantDto(p.getId(), p.getSupplierCode(), p.getBidAmount(),
            p.getTechnicalScore(), p.getFinancialScore(), p.getCombinedScore(),
            p.getQualified(), p.getRank());
    }

    public static ScoreDto toDto(CriterionScore s) {
        return new ScoreDto(s.getId(), s.getParticipantEvaluation().getSupplierCode(),
            s.getCriterion().getCode(), s.getScore(), s.getComment());
    }
}
