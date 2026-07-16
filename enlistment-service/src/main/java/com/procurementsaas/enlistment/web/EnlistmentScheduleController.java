package com.procurementsaas.enlistment.web;

import com.procurementsaas.enlistment.dto.Dtos.AddCriterionRequest;
import com.procurementsaas.enlistment.dto.Dtos.ApplicationDto;
import com.procurementsaas.enlistment.dto.Dtos.ApplyRequest;
import com.procurementsaas.enlistment.dto.Dtos.AssessRequest;
import com.procurementsaas.enlistment.dto.Dtos.AssessmentDto;
import com.procurementsaas.enlistment.dto.Dtos.CreateScheduleRequest;
import com.procurementsaas.enlistment.dto.Dtos.CriterionDto;
import com.procurementsaas.enlistment.dto.Dtos.ScheduleDto;
import com.procurementsaas.enlistment.service.EnlistmentScheduleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/enlistment-schedules")
public class EnlistmentScheduleController {

    private final EnlistmentScheduleService scheduleService;

    public EnlistmentScheduleController(EnlistmentScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('FEATURE_ENLISTMENT_VIEW')")
    public List<ScheduleDto> list(@RequestParam(required = false) String status) {
        return scheduleService.list(status);
    }

    @GetMapping("/{code}")
    @PreAuthorize("hasAuthority('FEATURE_ENLISTMENT_VIEW')")
    public ScheduleDto get(@PathVariable String code) {
        return scheduleService.get(code);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FEATURE_ENLISTMENT_MANAGE')")
    public ScheduleDto create(@Valid @RequestBody CreateScheduleRequest request) {
        return scheduleService.create(request);
    }

    @GetMapping("/{code}/criteria")
    @PreAuthorize("hasAuthority('FEATURE_ENLISTMENT_VIEW')")
    public List<CriterionDto> listCriteria(@PathVariable String code) {
        return scheduleService.listCriteria(code);
    }

    @PostMapping("/{code}/criteria")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FEATURE_ENLISTMENT_MANAGE')")
    public CriterionDto addCriterion(@PathVariable String code,
                                     @Valid @RequestBody AddCriterionRequest request) {
        return scheduleService.addCriterion(code, request);
    }

    @PostMapping("/{code}/publish")
    @PreAuthorize("hasAuthority('FEATURE_ENLISTMENT_MANAGE')")
    public ScheduleDto publish(@PathVariable String code) {
        return scheduleService.publish(code);
    }

    @PostMapping("/{code}/close")
    @PreAuthorize("hasAuthority('FEATURE_ENLISTMENT_MANAGE')")
    public ScheduleDto close(@PathVariable String code) {
        return scheduleService.close(code);
    }

    @PostMapping("/{code}/complete")
    @PreAuthorize("hasAuthority('FEATURE_ENLISTMENT_MANAGE')")
    public ScheduleDto complete(@PathVariable String code) {
        return scheduleService.complete(code);
    }

    @PostMapping("/{code}/cancel")
    @PreAuthorize("hasAuthority('FEATURE_ENLISTMENT_MANAGE')")
    public ScheduleDto cancel(@PathVariable String code) {
        return scheduleService.cancel(code);
    }

    // --- Applications --------------------------------------------------------

    @GetMapping("/{code}/applications")
    @PreAuthorize("hasAuthority('FEATURE_ENLISTMENT_VIEW')")
    public List<ApplicationDto> listApplications(@PathVariable String code) {
        return scheduleService.listApplications(code);
    }

    /** A supplier-side action, distinct from running the round. */
    @PostMapping("/{code}/applications")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FEATURE_ENLISTMENT_APPLY')")
    public ApplicationDto apply(@PathVariable String code, @Valid @RequestBody ApplyRequest request) {
        return scheduleService.apply(code, request);
    }

    @PostMapping("/{code}/applications/{supplierCode}/withdraw")
    @PreAuthorize("hasAuthority('FEATURE_ENLISTMENT_APPLY')")
    public ApplicationDto withdraw(@PathVariable String code, @PathVariable String supplierCode) {
        return scheduleService.withdraw(code, supplierCode);
    }

    // --- Assessment ----------------------------------------------------------

    @GetMapping("/{code}/applications/{supplierCode}/assessments")
    @PreAuthorize("hasAuthority('FEATURE_ENLISTMENT_VIEW')")
    public List<AssessmentDto> listAssessments(@PathVariable String code,
                                               @PathVariable String supplierCode) {
        return scheduleService.listAssessments(code, supplierCode);
    }

    /** Judging applicants is a committee action, separate from administering the round. */
    @PutMapping("/{code}/applications/{supplierCode}/assessments")
    @PreAuthorize("hasAuthority('FEATURE_ENLISTMENT_ASSESS')")
    public AssessmentDto assess(@PathVariable String code, @PathVariable String supplierCode,
                                @Valid @RequestBody AssessRequest request) {
        return scheduleService.assess(code, supplierCode, request);
    }

    @PostMapping("/{code}/applications/{supplierCode}/decide")
    @PreAuthorize("hasAuthority('FEATURE_ENLISTMENT_ASSESS')")
    public ApplicationDto decide(@PathVariable String code, @PathVariable String supplierCode) {
        return scheduleService.decide(code, supplierCode);
    }
}
