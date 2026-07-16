package com.procurementsaas.workflow.web;

import com.procurementsaas.workflow.dto.Dtos.AddStepRequest;
import com.procurementsaas.workflow.dto.Dtos.CreateWorkflowRequest;
import com.procurementsaas.workflow.dto.Dtos.StepDto;
import com.procurementsaas.workflow.dto.Dtos.WorkflowDto;
import com.procurementsaas.workflow.service.WorkflowService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Configuring who must approve what. An administrative concern, not an approving one. */
@RestController
@RequestMapping("/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('FEATURE_WORKFLOW_VIEW')")
    public List<WorkflowDto> list(@RequestParam(required = false) String subjectType) {
        return workflowService.list(subjectType);
    }

    @GetMapping("/{code}")
    @PreAuthorize("hasAuthority('FEATURE_WORKFLOW_VIEW')")
    public WorkflowDto get(@PathVariable String code) {
        return workflowService.get(code);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FEATURE_WORKFLOW_MANAGE')")
    public WorkflowDto create(@Valid @RequestBody CreateWorkflowRequest request) {
        return workflowService.create(request);
    }

    @PostMapping("/{code}/steps")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FEATURE_WORKFLOW_MANAGE')")
    public StepDto addStep(@PathVariable String code, @Valid @RequestBody AddStepRequest request) {
        return workflowService.addStep(code, request);
    }

    @PostMapping("/{code}/deactivate")
    @PreAuthorize("hasAuthority('FEATURE_WORKFLOW_MANAGE')")
    public WorkflowDto deactivate(@PathVariable String code) {
        return workflowService.deactivate(code);
    }
}
