package com.procurementsaas.reporting.service;

import com.procurementsaas.common.tenancy.TenantContext;
import com.procurementsaas.common.web.NotFoundException;
import com.procurementsaas.reporting.domain.ReportDefinition;
import com.procurementsaas.reporting.domain.ReportFormat;
import com.procurementsaas.reporting.domain.ReportJob;
import com.procurementsaas.reporting.dto.Dtos.DefinitionDto;
import com.procurementsaas.reporting.dto.Dtos.JobDto;
import com.procurementsaas.reporting.dto.Dtos.RunReportRequest;
import com.procurementsaas.reporting.engine.ReportDataProvider;
import com.procurementsaas.reporting.engine.ReportStorage;
import com.procurementsaas.reporting.repo.ReportDefinitionRepository;
import com.procurementsaas.reporting.repo.ReportJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Orchestrates report production: accept a request, hand it to a worker, render, store.
 *
 * <p>The request returns immediately with a queued job. The client polls the job and
 * downloads the file once it is ready — a year-long comparative statement has no business
 * being rendered inside an HTTP request.
 */
@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final ReportDefinitionRepository definitionRepository;
    private final ReportJobRepository jobRepository;
    private final Map<String, ReportDataProvider> providers;
    private final ReportStorage storage;
    private final ReportWorker worker;

    public ReportService(ReportDefinitionRepository definitionRepository,
                         ReportJobRepository jobRepository,
                         Map<String, ReportDataProvider> providers,
                         ReportStorage storage,
                         ReportWorker worker) {
        this.definitionRepository = definitionRepository;
        this.jobRepository = jobRepository;
        this.providers = providers;
        this.storage = storage;
        this.worker = worker;
    }

    @Transactional(readOnly = true)
    public List<DefinitionDto> catalogue() {
        return definitionRepository.findAll().stream()
            .filter(ReportDefinition::isActive)
            .map(ReportService::toDto)
            .toList();
    }

    /**
     * Accepts a report request and queues it. Validates up front — an unknown report or a
     * misconfigured one should fail the request now, not surface as a failed job later.
     */
    @Transactional
    public JobDto request(String requestedBy, RunReportRequest request) {
        ReportDefinition definition = definitionRepository.findByCode(request.definitionCode())
            .orElseThrow(() -> new NotFoundException(
                "Report not found: " + request.definitionCode()));
        if (!definition.isActive()) {
            throw new IllegalArgumentException("Report is retired: " + definition.getCode());
        }
        if (!providers.containsKey(definition.getProviderCode())) {
            throw new IllegalStateException(
                "No data provider for report " + definition.getCode());
        }

        // The tenant is resolved now, on the request thread, and handed explicitly to the
        // worker. A ThreadLocal does not follow work onto another thread, so relying on
        // TenantContext inside the worker would render every report against the default
        // schema — the classic, silent, cross-tenant data leak.
        String tenantId = TenantContext.getTenant();
        ReportJob job = new ReportJob(definition.getCode(), serialize(request.parameters()),
            requestedBy, tenantId);
        job = jobRepository.save(job);

        worker.render(job.getId(), definition.getCode(), request.parameters(), tenantId);
        return toDto(job);
    }

    @Transactional(readOnly = true)
    public JobDto getJob(Long id) {
        return toDto(findJob(id));
    }

    @Transactional(readOnly = true)
    public List<JobDto> recentJobs() {
        return jobRepository.findTop50ByOrderByCreatedAtDesc().stream()
            .map(ReportService::toDto).toList();
    }

    /** The rendered bytes of a completed job, for the download endpoint. */
    @Transactional(readOnly = true)
    public DownloadedReport download(Long id) {
        ReportJob job = findJob(id);
        if (!job.isDownloadable()) {
            throw new IllegalStateException(
                "Report " + id + " is not ready (status " + job.getStatus() + ")");
        }
        byte[] content = storage.retrieve(job.getStorageKey());
        ReportFormat format = definitionRepository.findByCode(job.getDefinitionCode())
            .map(ReportDefinition::getFormat)
            .orElse(ReportFormat.CSV);
        String filename = job.getDefinitionCode() + "." + format.extension();
        return new DownloadedReport(filename, format.contentType(), content);
    }

    public record DownloadedReport(String filename, String contentType, byte[] content) {
    }

    private ReportJob findJob(Long id) {
        return jobRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Report job not found: " + id));
    }

    /** Minimal, dependency-free serialisation of the filter map for audit/re-run. */
    private static String serialize(Map<String, String> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return null;
        }
        return parameters.entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .reduce((a, b) -> a + ";" + b)
            .orElse(null);
    }

    private static DefinitionDto toDto(ReportDefinition d) {
        return new DefinitionDto(d.getId(), d.getCode(), d.getName(), d.getDescription(),
            d.getFormat().name(), d.isActive());
    }

    private static JobDto toDto(ReportJob j) {
        return new JobDto(j.getId(), j.getDefinitionCode(), j.getStatus().name(),
            j.getRequestedBy(), j.getRowCount(), j.getSizeBytes(), j.getError(),
            j.getCreatedAt(), j.getCompletedAt(), j.isDownloadable());
    }
}
