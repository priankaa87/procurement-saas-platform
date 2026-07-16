package com.procurementsaas.enlistment.service;

import com.procurementsaas.common.web.NotFoundException;
import com.procurementsaas.enlistment.domain.ApplicationStatus;
import com.procurementsaas.enlistment.domain.CriterionAssessment;
import com.procurementsaas.enlistment.domain.Enlistment;
import com.procurementsaas.enlistment.domain.EnlistmentApplication;
import com.procurementsaas.enlistment.domain.EnlistmentCriterion;
import com.procurementsaas.enlistment.domain.EnlistmentSchedule;
import com.procurementsaas.enlistment.domain.ScheduleStatus;
import com.procurementsaas.enlistment.dto.Dtos.AddCriterionRequest;
import com.procurementsaas.enlistment.dto.Dtos.ApplicationDto;
import com.procurementsaas.enlistment.dto.Dtos.ApplyRequest;
import com.procurementsaas.enlistment.dto.Dtos.AssessRequest;
import com.procurementsaas.enlistment.dto.Dtos.AssessmentDto;
import com.procurementsaas.enlistment.dto.Dtos.CreateScheduleRequest;
import com.procurementsaas.enlistment.dto.Dtos.CriterionDto;
import com.procurementsaas.enlistment.dto.Dtos.ScheduleDto;
import com.procurementsaas.enlistment.repo.CriterionAssessmentRepository;
import com.procurementsaas.enlistment.repo.EnlistmentApplicationRepository;
import com.procurementsaas.enlistment.repo.EnlistmentCriterionRepository;
import com.procurementsaas.enlistment.repo.EnlistmentRepository;
import com.procurementsaas.enlistment.repo.EnlistmentScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Running a pre-qualification round: criteria, applications, assessment, decision. */
@Service
@Transactional
public class EnlistmentScheduleService {

    private static final Logger log = LoggerFactory.getLogger(EnlistmentScheduleService.class);

    private final EnlistmentScheduleRepository scheduleRepository;
    private final EnlistmentCriterionRepository criterionRepository;
    private final EnlistmentApplicationRepository applicationRepository;
    private final CriterionAssessmentRepository assessmentRepository;
    private final EnlistmentRepository enlistmentRepository;

    public EnlistmentScheduleService(EnlistmentScheduleRepository scheduleRepository,
                                     EnlistmentCriterionRepository criterionRepository,
                                     EnlistmentApplicationRepository applicationRepository,
                                     CriterionAssessmentRepository assessmentRepository,
                                     EnlistmentRepository enlistmentRepository) {
        this.scheduleRepository = scheduleRepository;
        this.criterionRepository = criterionRepository;
        this.applicationRepository = applicationRepository;
        this.assessmentRepository = assessmentRepository;
        this.enlistmentRepository = enlistmentRepository;
    }

    // --- Schedule setup ------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ScheduleDto> list(String status) {
        List<EnlistmentSchedule> schedules = (status == null)
            ? scheduleRepository.findAll()
            : scheduleRepository.findByStatus(parseStatus(status));
        return schedules.stream().map(this::withCriteriaCount).toList();
    }

    @Transactional(readOnly = true)
    public ScheduleDto get(String code) {
        return withCriteriaCount(findSchedule(code));
    }

    public ScheduleDto create(CreateScheduleRequest request) {
        if (scheduleRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Schedule code already exists: " + request.code());
        }
        EnlistmentSchedule schedule = new EnlistmentSchedule(request.code(), request.title(),
            request.description(), request.categoryCode(), request.applicationDeadline(),
            request.passMark(), request.validityMonths());
        return withCriteriaCount(scheduleRepository.save(schedule));
    }

    @Transactional(readOnly = true)
    public List<CriterionDto> listCriteria(String code) {
        EnlistmentSchedule schedule = findSchedule(code);
        return criterionRepository.findByScheduleId(schedule.getId()).stream()
            .map(EnlistmentMapper::toDto).toList();
    }

    /** Criteria are fixed at publication: the standard cannot move once people are judged by it. */
    public CriterionDto addCriterion(String code, AddCriterionRequest request) {
        EnlistmentSchedule schedule = findSchedule(code);
        if (!schedule.isEditable()) {
            throw new IllegalStateException(
                "Criteria cannot change once " + code + " is published; applicants are being "
                    + "judged against the published standard");
        }
        if (criterionRepository.existsByScheduleIdAndCode(schedule.getId(), request.code())) {
            throw new IllegalArgumentException("Criterion already exists: " + request.code());
        }
        EnlistmentCriterion criterion = new EnlistmentCriterion(schedule, request.code(),
            request.name(), request.weight(), request.mandatory());
        return EnlistmentMapper.toDto(criterionRepository.save(criterion));
    }

    public ScheduleDto publish(String code) {
        EnlistmentSchedule schedule = findSchedule(code);
        long criteria = criterionRepository.countByScheduleId(schedule.getId());
        int totalWeight = criterionRepository.findByScheduleId(schedule.getId()).stream()
            .mapToInt(EnlistmentCriterion::getWeight).sum();
        if (criteria > 0 && totalWeight != 100) {
            throw new IllegalStateException(
                "Criteria weights must total 100 but total " + totalWeight);
        }
        schedule.publish(criteria);
        return withCriteriaCount(scheduleRepository.save(schedule));
    }

    public ScheduleDto close(String code) {
        EnlistmentSchedule schedule = findSchedule(code);
        schedule.close();
        return withCriteriaCount(scheduleRepository.save(schedule));
    }

    public ScheduleDto cancel(String code) {
        EnlistmentSchedule schedule = findSchedule(code);
        schedule.cancel();
        return withCriteriaCount(scheduleRepository.save(schedule));
    }

    // --- Applications --------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ApplicationDto> listApplications(String code) {
        EnlistmentSchedule schedule = findSchedule(code);
        return applicationRepository.findByScheduleId(schedule.getId()).stream()
            .map(EnlistmentMapper::toDto).toList();
    }

    /** Applications are only accepted while the round is open and before the deadline. */
    public ApplicationDto apply(String code, ApplyRequest request) {
        EnlistmentSchedule schedule = findSchedule(code);
        if (!schedule.acceptingApplications()) {
            throw new IllegalStateException(
                "Schedule " + code + " is not accepting applications (status "
                    + schedule.getStatus() + ", deadline " + schedule.getApplicationDeadline() + ")");
        }
        if (applicationRepository.existsByScheduleIdAndSupplierCode(schedule.getId(),
            request.supplierCode())) {
            throw new IllegalArgumentException(
                "Supplier has already applied: " + request.supplierCode());
        }
        EnlistmentApplication application =
            new EnlistmentApplication(schedule, request.supplierCode());
        return EnlistmentMapper.toDto(applicationRepository.save(application));
    }

    public ApplicationDto withdraw(String code, String supplierCode) {
        EnlistmentApplication application = findApplication(code, supplierCode);
        application.withdraw();
        return EnlistmentMapper.toDto(applicationRepository.save(application));
    }

    // --- Assessment ----------------------------------------------------------

    @Transactional(readOnly = true)
    public List<AssessmentDto> listAssessments(String code, String supplierCode) {
        EnlistmentApplication application = findApplication(code, supplierCode);
        return assessmentRepository.findByApplicationId(application.getId()).stream()
            .map(EnlistmentMapper::toDto).toList();
    }

    /** Assessment happens after the round closes, never while applications are still arriving. */
    public AssessmentDto assess(String code, String supplierCode, AssessRequest request) {
        EnlistmentSchedule schedule = findSchedule(code);
        if (!schedule.underAssessment()) {
            throw new IllegalStateException(
                "Assessment is only possible once " + code + " is closed; it is "
                    + schedule.getStatus());
        }
        EnlistmentApplication application = findApplication(code, supplierCode);
        EnlistmentCriterion criterion = criterionRepository
            .findByScheduleIdAndCode(schedule.getId(), request.criterionCode())
            .orElseThrow(() -> new NotFoundException(
                "Criterion not found: " + request.criterionCode()));

        CriterionAssessment assessment = assessmentRepository
            .findByApplicationIdAndCriterionId(application.getId(), criterion.getId())
            .orElse(null);
        if (assessment == null) {
            assessment = new CriterionAssessment(application, criterion, request.score(),
                request.met(), request.comment());
        } else {
            assessment.update(request.score(), request.met(), request.comment());
        }
        return EnlistmentMapper.toDto(assessmentRepository.save(assessment));
    }

    /**
     * Decides an application and, if it succeeds, enlists the supplier.
     *
     * <p>Refuses to decide until every criterion has been assessed: a decision made on a
     * partial reading of the evidence is not a decision, it is a guess.
     */
    public ApplicationDto decide(String code, String supplierCode) {
        EnlistmentSchedule schedule = findSchedule(code);
        if (!schedule.underAssessment()) {
            throw new IllegalStateException(
                "Applications can only be decided once " + code + " is closed");
        }
        EnlistmentApplication application = findApplication(code, supplierCode);

        List<EnlistmentCriterion> criteria = criterionRepository.findByScheduleId(schedule.getId());
        List<CriterionAssessment> assessments =
            assessmentRepository.findByApplicationId(application.getId());
        if (assessments.size() != criteria.size()) {
            throw new IllegalStateException("Supplier " + supplierCode + " has been assessed on "
                + assessments.size() + " of " + criteria.size() + " criteria");
        }

        QualificationDecision.Result result =
            QualificationDecision.decide(assessments, schedule.getPassMark());

        if (result.qualified()) {
            application.qualify(result.score());
            enlistmentRepository.save(new Enlistment(supplierCode, schedule.getCategoryCode(),
                schedule.getCode(), LocalDate.now(), schedule.getValidityMonths()));
            log.info("Supplier {} enlisted for {} via {} (score {})", supplierCode,
                schedule.getCategoryCode(), code, result.score());
        } else {
            application.reject(result.score(), result.reason());
            log.info("Supplier {} rejected from {}: {}", supplierCode, code, result.reason());
        }
        return EnlistmentMapper.toDto(applicationRepository.save(application));
    }

    /** Completes the round once nothing is left undecided. */
    public ScheduleDto complete(String code) {
        EnlistmentSchedule schedule = findSchedule(code);
        long undecided = applicationRepository.countByScheduleIdAndStatus(schedule.getId(),
            ApplicationStatus.SUBMITTED);
        if (undecided > 0) {
            throw new IllegalStateException(
                undecided + " application(s) on " + code + " are still undecided");
        }
        schedule.complete();
        return withCriteriaCount(scheduleRepository.save(schedule));
    }

    // --- Helpers -------------------------------------------------------------

    private EnlistmentSchedule findSchedule(String code) {
        return scheduleRepository.findByCode(code)
            .orElseThrow(() -> new NotFoundException("Schedule not found: " + code));
    }

    private EnlistmentApplication findApplication(String code, String supplierCode) {
        EnlistmentSchedule schedule = findSchedule(code);
        return applicationRepository
            .findByScheduleIdAndSupplierCode(schedule.getId(), supplierCode)
            .orElseThrow(() -> new NotFoundException(
                "No application by " + supplierCode + " on " + code));
    }

    private ScheduleDto withCriteriaCount(EnlistmentSchedule schedule) {
        return EnlistmentMapper.toDto(schedule,
            criterionRepository.countByScheduleId(schedule.getId()));
    }

    private static ScheduleStatus parseStatus(String status) {
        try {
            return ScheduleStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown schedule status: " + status);
        }
    }
}
