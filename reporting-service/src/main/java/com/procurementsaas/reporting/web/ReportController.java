package com.procurementsaas.reporting.web;

import com.procurementsaas.reporting.dto.Dtos.DefinitionDto;
import com.procurementsaas.reporting.dto.Dtos.JobDto;
import com.procurementsaas.reporting.dto.Dtos.RunReportRequest;
import com.procurementsaas.reporting.service.ReportService;
import com.procurementsaas.reporting.service.ReportService.DownloadedReport;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** What can be produced. */
    @GetMapping("/definitions")
    @PreAuthorize("hasAuthority('FEATURE_REPORT_VIEW')")
    public List<DefinitionDto> catalogue() {
        return reportService.catalogue();
    }

    /** Queues a report and returns the job immediately — 202, because it is not done yet. */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAuthority('FEATURE_REPORT_RUN')")
    public JobDto run(@Valid @RequestBody RunReportRequest request, Authentication authentication) {
        String requestedBy = authentication == null ? "anonymous" : authentication.getName();
        return reportService.request(requestedBy, request);
    }

    @GetMapping("/jobs")
    @PreAuthorize("hasAuthority('FEATURE_REPORT_VIEW')")
    public List<JobDto> recentJobs() {
        return reportService.recentJobs();
    }

    /** Poll this until {@code downloadable} is true (or {@code status} is FAILED). */
    @GetMapping("/jobs/{id}")
    @PreAuthorize("hasAuthority('FEATURE_REPORT_VIEW')")
    public JobDto job(@PathVariable Long id) {
        return reportService.getJob(id);
    }

    /** The finished file. Refused with 409 until the job has completed. */
    @GetMapping("/jobs/{id}/download")
    @PreAuthorize("hasAuthority('FEATURE_REPORT_VIEW')")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        DownloadedReport report = reportService.download(id);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + report.filename() + "\"")
            .contentType(MediaType.parseMediaType(report.contentType()))
            .contentLength(report.content().length)
            .body(new ByteArrayResource(report.content()));
    }
}
