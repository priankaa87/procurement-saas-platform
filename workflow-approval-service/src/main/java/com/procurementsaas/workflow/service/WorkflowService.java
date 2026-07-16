package com.procurementsaas.workflow.service;

import com.procurementsaas.common.web.NotFoundException;
import com.procurementsaas.workflow.domain.ApprovalStep;
import com.procurementsaas.workflow.domain.ApprovalWorkflow;
import com.procurementsaas.workflow.dto.Dtos.AddStepRequest;
import com.procurementsaas.workflow.dto.Dtos.CreateWorkflowRequest;
import com.procurementsaas.workflow.dto.Dtos.StepDto;
import com.procurementsaas.workflow.dto.Dtos.WorkflowDto;
import com.procurementsaas.workflow.repo.ApprovalStepRepository;
import com.procurementsaas.workflow.repo.ApprovalWorkflowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Configuring approval workflows and their steps. */
@Service
@Transactional
public class WorkflowService {

    private final ApprovalWorkflowRepository workflowRepository;
    private final ApprovalStepRepository stepRepository;

    public WorkflowService(ApprovalWorkflowRepository workflowRepository,
                           ApprovalStepRepository stepRepository) {
        this.workflowRepository = workflowRepository;
        this.stepRepository = stepRepository;
    }

    @Transactional(readOnly = true)
    public List<WorkflowDto> list(String subjectType) {
        List<ApprovalWorkflow> workflows = (subjectType == null)
            ? workflowRepository.findAll()
            : workflowRepository.findBySubjectType(subjectType);
        return workflows.stream().map(this::withSteps).toList();
    }

    @Transactional(readOnly = true)
    public WorkflowDto get(String code) {
        return withSteps(findWorkflow(code));
    }

    public WorkflowDto create(CreateWorkflowRequest request) {
        if (workflowRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Workflow code already exists: " + request.code());
        }
        ApprovalWorkflow workflow = new ApprovalWorkflow(request.code(), request.name(),
            request.subjectType());
        return withSteps(workflowRepository.save(workflow));
    }

    public StepDto addStep(String code, AddStepRequest request) {
        ApprovalWorkflow workflow = findWorkflow(code);
        if (stepRepository.findByWorkflowIdAndStepNo(workflow.getId(), request.stepNo()).isPresent()) {
            throw new IllegalArgumentException("Step number already used: " + request.stepNo());
        }
        ApprovalStep step = new ApprovalStep(workflow, request.stepNo(), request.name(),
            request.roleCode());
        return WorkflowMapper.toDto(stepRepository.save(step));
    }

    /**
     * Retires a workflow. Deactivated rather than deleted, because requests already raised
     * against it must remain readable.
     */
    public WorkflowDto deactivate(String code) {
        ApprovalWorkflow workflow = findWorkflow(code);
        workflow.deactivate();
        return withSteps(workflowRepository.save(workflow));
    }

    private ApprovalWorkflow findWorkflow(String code) {
        return workflowRepository.findByCode(code)
            .orElseThrow(() -> new NotFoundException("Workflow not found: " + code));
    }

    private WorkflowDto withSteps(ApprovalWorkflow workflow) {
        return WorkflowMapper.toDto(workflow,
            stepRepository.findByWorkflowIdOrderByStepNo(workflow.getId()));
    }
}
