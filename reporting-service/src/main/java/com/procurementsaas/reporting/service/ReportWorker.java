package com.procurementsaas.reporting.service;

import com.procurementsaas.common.tenancy.TenantContext;
import com.procurementsaas.reporting.domain.ReportDefinition;
import com.procurementsaas.reporting.domain.ReportFormat;
import com.procurementsaas.reporting.domain.ReportJob;
import com.procurementsaas.reporting.engine.ReportData;
import com.procurementsaas.reporting.engine.ReportDataProvider;
import com.procurementsaas.reporting.engine.ReportRenderer;
import com.procurementsaas.reporting.engine.ReportStorage;
import com.procurementsaas.reporting.repo.ReportDefinitionRepository;
import com.procurementsaas.reporting.repo.ReportJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Renders a queued report on a worker thread.
 *
 * <p>Deliberately a separate bean from {@code ReportService}. Spring's {@code @Async} works
 * through a proxy, and a proxy is only involved when one bean calls another — a service
 * calling its own {@code @Async} method would run it inline on the request thread, quietly
 * defeating the entire point of doing this asynchronously.
 */
@Component
public class ReportWorker {

    private static final Logger log = LoggerFactory.getLogger(ReportWorker.class);

    private final ReportDefinitionRepository definitionRepository;
    private final ReportJobRepository jobRepository;
    private final Map<ReportFormat, ReportRenderer> renderers;
    private final Map<String, ReportDataProvider> providers;
    private final ReportStorage storage;

    public ReportWorker(ReportDefinitionRepository definitionRepository,
                        ReportJobRepository jobRepository,
                        List<ReportRenderer> renderers,
                        List<ReportDataProvider> providers,
                        ReportStorage storage) {
        this.definitionRepository = definitionRepository;
        this.jobRepository = jobRepository;
        this.renderers = renderers.stream()
            .collect(Collectors.toMap(ReportRenderer::format, Function.identity()));
        this.providers = providers.stream()
            .collect(Collectors.toMap(ReportDataProvider::code, Function.identity()));
        this.storage = storage;
    }

    /**
     * Produces the file. Runs in its own transaction, because the request that created the
     * job committed and returned long before this starts.
     *
     * @param tenantId passed explicitly, not read from a ThreadLocal — a ThreadLocal does
     *                 not follow work onto a worker thread, and defaulting to the shared
     *                 schema here would render one tenant's report against another's data
     */
    @Async("reportExecutor")
    @Transactional
    public void render(Long jobId, String definitionCode, Map<String, String> parameters,
                       String tenantId) {
        TenantContext.setTenant(tenantId);
        try {
            ReportJob job = jobRepository.findById(jobId).orElseThrow();
            job.markRunning();
            jobRepository.save(job);

            ReportDefinition definition = definitionRepository.findByCode(definitionCode).orElseThrow();
            try {
                ReportDataProvider provider = providers.get(definition.getProviderCode());
                ReportRenderer renderer = renderers.get(definition.getFormat());

                ReportData data = provider.fetch(parameters == null ? Map.of() : parameters);
                byte[] rendered = renderer.render(data);
                String filename = definition.getCode() + "." + definition.getFormat().extension();
                String key = storage.store(tenantId, jobId, filename, rendered);

                job.markCompleted(key, rendered.length, data.rowCount());
                log.info("Report {} completed: {} rows, {} bytes", jobId, data.rowCount(),
                    rendered.length);
            } catch (RuntimeException ex) {
                // A rendering failure lands on the job, rather than vanishing into a worker
                // thread. Whoever asked for the report looks here to learn why it never came.
                log.warn("Report {} failed", jobId, ex);
                job.markFailed(ex.getMessage());
            }
            jobRepository.save(job);
        } finally {
            TenantContext.clear();
        }
    }
}
