package com.procurementsaas.evaluation.web;

import com.procurementsaas.evaluation.dto.Dtos.AddCriterionRequest;
import com.procurementsaas.evaluation.dto.Dtos.AddParticipantRequest;
import com.procurementsaas.evaluation.dto.Dtos.ComparativeStatementDto;
import com.procurementsaas.evaluation.dto.Dtos.CreateEvaluationRequest;
import com.procurementsaas.evaluation.dto.Dtos.CriterionDto;
import com.procurementsaas.evaluation.dto.Dtos.EvaluationDto;
import com.procurementsaas.evaluation.dto.Dtos.ParticipantDto;
import com.procurementsaas.evaluation.dto.Dtos.ScoreDto;
import com.procurementsaas.evaluation.dto.Dtos.ScoreRequest;
import com.procurementsaas.evaluation.service.EvaluationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/evaluations")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FEATURE_EVAL_MANAGE')")
    public EvaluationDto create(@Valid @RequestBody CreateEvaluationRequest request) {
        return evaluationService.create(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FEATURE_EVAL_VIEW')")
    public EvaluationDto get(@PathVariable Long id) {
        return evaluationService.get(id);
    }

    @GetMapping("/{id}/criteria")
    @PreAuthorize("hasAuthority('FEATURE_EVAL_VIEW')")
    public List<CriterionDto> listCriteria(@PathVariable Long id) {
        return evaluationService.listCriteria(id);
    }

    @PostMapping("/{id}/criteria")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FEATURE_EVAL_MANAGE')")
    public CriterionDto addCriterion(@PathVariable Long id,
                                     @Valid @RequestBody AddCriterionRequest request) {
        return evaluationService.addCriterion(id, request);
    }

    @GetMapping("/{id}/participants")
    @PreAuthorize("hasAuthority('FEATURE_EVAL_VIEW')")
    public List<ParticipantDto> listParticipants(@PathVariable Long id) {
        return evaluationService.listParticipants(id);
    }

    @PostMapping("/{id}/participants")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FEATURE_EVAL_MANAGE')")
    public ParticipantDto addParticipant(@PathVariable Long id,
                                         @Valid @RequestBody AddParticipantRequest request) {
        return evaluationService.addParticipant(id, request);
    }

    /** Scoring is a committee action, separate from configuring the evaluation. */
    @PutMapping("/{id}/scores")
    @PreAuthorize("hasAuthority('FEATURE_EVAL_SCORE')")
    public ScoreDto score(@PathVariable Long id, @Valid @RequestBody ScoreRequest request) {
        return evaluationService.score(id, request);
    }

    @PostMapping("/{id}/close-technical")
    @PreAuthorize("hasAuthority('FEATURE_EVAL_MANAGE')")
    public List<ParticipantDto> closeTechnical(@PathVariable Long id) {
        return evaluationService.closeTechnical(id);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('FEATURE_EVAL_MANAGE')")
    public List<ParticipantDto> complete(@PathVariable Long id) {
        return evaluationService.complete(id);
    }

    /** Refused with 409 until the evaluation is complete. */
    @GetMapping("/{id}/comparative-statement")
    @PreAuthorize("hasAuthority('FEATURE_EVAL_VIEW')")
    public ComparativeStatementDto comparativeStatement(@PathVariable Long id) {
        return evaluationService.comparativeStatement(id);
    }
}
