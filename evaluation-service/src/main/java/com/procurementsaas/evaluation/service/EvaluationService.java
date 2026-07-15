package com.procurementsaas.evaluation.service;

import com.procurementsaas.common.web.NotFoundException;
import com.procurementsaas.evaluation.domain.CriterionScore;
import com.procurementsaas.evaluation.domain.Evaluation;
import com.procurementsaas.evaluation.domain.EvaluationCriterion;
import com.procurementsaas.evaluation.domain.EvaluationStatus;
import com.procurementsaas.evaluation.domain.ParticipantEvaluation;
import com.procurementsaas.evaluation.dto.Dtos.AddCriterionRequest;
import com.procurementsaas.evaluation.dto.Dtos.AddParticipantRequest;
import com.procurementsaas.evaluation.dto.Dtos.ComparativeRow;
import com.procurementsaas.evaluation.dto.Dtos.ComparativeStatementDto;
import com.procurementsaas.evaluation.dto.Dtos.CreateEvaluationRequest;
import com.procurementsaas.evaluation.dto.Dtos.CriterionDto;
import com.procurementsaas.evaluation.dto.Dtos.EvaluationDto;
import com.procurementsaas.evaluation.dto.Dtos.ParticipantDto;
import com.procurementsaas.evaluation.dto.Dtos.ScoreDto;
import com.procurementsaas.evaluation.dto.Dtos.ScoreRequest;
import com.procurementsaas.evaluation.repo.CriterionScoreRepository;
import com.procurementsaas.evaluation.repo.EvaluationCriterionRepository;
import com.procurementsaas.evaluation.repo.EvaluationRepository;
import com.procurementsaas.evaluation.repo.ParticipantEvaluationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs the two-stage evaluation: weighted technical scoring with a pass mark, then
 * financial scoring of the survivors, then a combined ranking.
 */
@Service
@Transactional
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final EvaluationCriterionRepository criterionRepository;
    private final ParticipantEvaluationRepository participantRepository;
    private final CriterionScoreRepository scoreRepository;

    public EvaluationService(EvaluationRepository evaluationRepository,
                             EvaluationCriterionRepository criterionRepository,
                             ParticipantEvaluationRepository participantRepository,
                             CriterionScoreRepository scoreRepository) {
        this.evaluationRepository = evaluationRepository;
        this.criterionRepository = criterionRepository;
        this.participantRepository = participantRepository;
        this.scoreRepository = scoreRepository;
    }

    // --- Setup ---------------------------------------------------------------

    public EvaluationDto create(CreateEvaluationRequest request) {
        if (evaluationRepository.existsByTenderCode(request.tenderCode())) {
            throw new IllegalArgumentException(
                "An evaluation already exists for tender " + request.tenderCode());
        }
        Evaluation evaluation = new Evaluation(request.tenderCode(), request.technicalWeight(),
            request.financialWeight(), request.passMark());
        return EvaluationMapper.toDto(evaluationRepository.save(evaluation));
    }

    @Transactional(readOnly = true)
    public EvaluationDto get(Long id) {
        return EvaluationMapper.toDto(findEvaluation(id));
    }

    @Transactional(readOnly = true)
    public List<CriterionDto> listCriteria(Long evaluationId) {
        findEvaluation(evaluationId);
        return criterionRepository.findByEvaluationId(evaluationId).stream()
            .map(EvaluationMapper::toDto).toList();
    }

    public CriterionDto addCriterion(Long evaluationId, AddCriterionRequest request) {
        Evaluation evaluation = findEditable(evaluationId);
        if (criterionRepository.existsByEvaluationIdAndCode(evaluationId, request.code())) {
            throw new IllegalArgumentException("Criterion already exists: " + request.code());
        }
        EvaluationCriterion criterion = new EvaluationCriterion(evaluation, request.code(),
            request.name(), request.weight());
        return EvaluationMapper.toDto(criterionRepository.save(criterion));
    }

    @Transactional(readOnly = true)
    public List<ParticipantDto> listParticipants(Long evaluationId) {
        findEvaluation(evaluationId);
        return participantRepository.findByEvaluationId(evaluationId).stream()
            .map(EvaluationMapper::toDto).toList();
    }

    public ParticipantDto addParticipant(Long evaluationId, AddParticipantRequest request) {
        Evaluation evaluation = findEditable(evaluationId);
        if (participantRepository.existsByEvaluationIdAndSupplierCode(evaluationId, request.supplierCode())) {
            throw new IllegalArgumentException(
                "Participant already added: " + request.supplierCode());
        }
        ParticipantEvaluation participant = new ParticipantEvaluation(evaluation,
            request.supplierCode(), request.bidAmount());
        return EvaluationMapper.toDto(participantRepository.save(participant));
    }

    // --- Technical stage -----------------------------------------------------

    /** Records (or revises) one participant's score against one criterion. */
    public ScoreDto score(Long evaluationId, ScoreRequest request) {
        findEditable(evaluationId);
        ParticipantEvaluation participant = findParticipant(evaluationId, request.supplierCode());
        EvaluationCriterion criterion = criterionRepository
            .findByEvaluationIdAndCode(evaluationId, request.criterionCode())
            .orElseThrow(() -> new NotFoundException(
                "Criterion not found: " + request.criterionCode()));

        CriterionScore score = scoreRepository
            .findByParticipantEvaluationIdAndCriterionId(participant.getId(), criterion.getId())
            .orElse(null);
        if (score == null) {
            score = new CriterionScore(participant, criterion, request.score(), request.comment());
        } else {
            score.update(request.score(), request.comment());
        }
        return EvaluationMapper.toDto(scoreRepository.save(score));
    }

    /**
     * Closes technical scoring: computes each participant's weighted score and decides
     * qualification against the pass mark. Refuses to close unless the criteria weights
     * total 100 and every participant has been scored on every criterion — a partial
     * evaluation must never be mistaken for a complete one.
     */
    public List<ParticipantDto> closeTechnical(Long evaluationId) {
        Evaluation evaluation = findEvaluation(evaluationId);
        List<EvaluationCriterion> criteria = criterionRepository.findByEvaluationId(evaluationId);
        List<ParticipantEvaluation> participants = participantRepository.findByEvaluationId(evaluationId);

        if (criteria.isEmpty()) {
            throw new IllegalStateException("No criteria defined for tender " + evaluation.getTenderCode());
        }
        if (participants.isEmpty()) {
            throw new IllegalStateException("No participants for tender " + evaluation.getTenderCode());
        }
        int totalWeight = criteria.stream().mapToInt(EvaluationCriterion::getWeight).sum();
        if (totalWeight != 100) {
            throw new IllegalStateException(
                "Criteria weights must total 100 but total " + totalWeight);
        }

        Map<Long, Integer> weights = new HashMap<>();
        criteria.forEach(c -> weights.put(c.getId(), c.getWeight()));

        for (ParticipantEvaluation participant : participants) {
            List<CriterionScore> scores = scoreRepository.findByParticipantEvaluationId(participant.getId());
            if (scores.size() != criteria.size()) {
                throw new IllegalStateException("Participant " + participant.getSupplierCode()
                    + " is missing scores: " + scores.size() + " of " + criteria.size());
            }
            Map<Long, BigDecimal> byCriterion = new HashMap<>();
            scores.forEach(s -> byCriterion.put(s.getCriterion().getId(), s.getScore()));

            BigDecimal technical = ScoreCalculator.technicalScore(byCriterion, weights);
            boolean qualified = technical.compareTo(evaluation.getPassMark()) >= 0;
            participant.applyTechnicalResult(technical, qualified);
            participantRepository.save(participant);
        }

        evaluation.closeTechnical();
        evaluationRepository.save(evaluation);
        return listParticipants(evaluationId);
    }

    // --- Financial stage -----------------------------------------------------

    /**
     * Scores the qualified participants financially and ranks them by combined score.
     * Disqualified participants are priced at nothing and left unranked — they are out.
     */
    public List<ParticipantDto> complete(Long evaluationId) {
        Evaluation evaluation = findEvaluation(evaluationId);
        List<ParticipantEvaluation> participants = participantRepository.findByEvaluationId(evaluationId);

        List<ParticipantEvaluation> qualified = participants.stream()
            .filter(p -> Boolean.TRUE.equals(p.getQualified()))
            .toList();
        if (qualified.isEmpty()) {
            throw new IllegalStateException(
                "No participant passed the technical stage for tender " + evaluation.getTenderCode());
        }

        BigDecimal lowestBid = qualified.stream()
            .map(ParticipantEvaluation::getBidAmount)
            .min(Comparator.naturalOrder())
            .orElseThrow();

        for (ParticipantEvaluation participant : qualified) {
            BigDecimal financial = ScoreCalculator.financialScore(participant.getBidAmount(), lowestBid);
            BigDecimal combined = ScoreCalculator.combinedScore(participant.getTechnicalScore(),
                evaluation.getTechnicalWeight(), financial, evaluation.getFinancialWeight());
            participant.applyFinancialResult(financial, combined);
        }

        List<ParticipantEvaluation> ranked = new ArrayList<>(qualified);
        ranked.sort(Comparator.comparing(ParticipantEvaluation::getCombinedScore).reversed()
            .thenComparing(ParticipantEvaluation::getBidAmount));
        for (int i = 0; i < ranked.size(); i++) {
            ranked.get(i).assignRank(i + 1);
            participantRepository.save(ranked.get(i));
        }

        evaluation.complete();
        evaluationRepository.save(evaluation);
        return listParticipants(evaluationId);
    }

    // --- Comparative statement -----------------------------------------------

    /**
     * The ranked comparison used to justify an award.
     *
     * @throws IllegalStateException until the evaluation is complete (surfaced as 409),
     *                               so a half-finished ranking is never circulated
     */
    @Transactional(readOnly = true)
    public ComparativeStatementDto comparativeStatement(Long evaluationId) {
        Evaluation evaluation = findEvaluation(evaluationId);
        if (evaluation.getStatus() != EvaluationStatus.COMPLETED) {
            throw new IllegalStateException(
                "The comparative statement is available once the evaluation is complete; "
                    + "current status is " + evaluation.getStatus());
        }
        List<ParticipantEvaluation> participants = participantRepository.findByEvaluationId(evaluationId);

        List<ComparativeRow> rows = participants.stream()
            .sorted(Comparator.comparing(
                (ParticipantEvaluation p) -> p.getRank() == null ? Integer.MAX_VALUE : p.getRank())
                .thenComparing(ParticipantEvaluation::getSupplierCode))
            .map(p -> new ComparativeRow(p.getRank(), p.getSupplierCode(), p.getBidAmount(),
                p.getTechnicalScore(), p.getFinancialScore(), p.getCombinedScore(),
                Boolean.TRUE.equals(p.getQualified())))
            .toList();

        String recommended = rows.stream()
            .filter(r -> Integer.valueOf(1).equals(r.rank()))
            .map(ComparativeRow::supplierCode)
            .findFirst()
            .orElse(null);

        return new ComparativeStatementDto(evaluation.getTenderCode(), evaluation.getStatus().name(),
            evaluation.getTechnicalWeight(), evaluation.getFinancialWeight(),
            evaluation.getPassMark(), recommended, rows);
    }

    // --- Helpers -------------------------------------------------------------

    private Evaluation findEvaluation(Long id) {
        return evaluationRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Evaluation not found: " + id));
    }

    /** Guards setup and technical scoring, which are frozen once the stage closes. */
    private Evaluation findEditable(Long id) {
        Evaluation evaluation = findEvaluation(id);
        if (!evaluation.isEditable()) {
            throw new IllegalStateException(
                "Evaluation is no longer editable (status " + evaluation.getStatus() + ")");
        }
        return evaluation;
    }

    private ParticipantEvaluation findParticipant(Long evaluationId, String supplierCode) {
        return participantRepository.findByEvaluationIdAndSupplierCode(evaluationId, supplierCode)
            .orElseThrow(() -> new NotFoundException("Participant not found: " + supplierCode));
    }
}
