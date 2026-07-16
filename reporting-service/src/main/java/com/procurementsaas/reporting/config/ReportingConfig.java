package com.procurementsaas.reporting.config;

import com.procurementsaas.reporting.domain.ReportFormat;
import com.procurementsaas.reporting.engine.ReportDataProvider;
import com.procurementsaas.reporting.engine.ReportRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Wires the reporting engine together: the worker pool, and lookups from a format to its
 * renderer and from a provider code to its provider.
 *
 * <p>Building the maps from every bean that implements the interface means adding a
 * renderer or provider is a matter of writing the class — nothing here has to be edited.
 */
@Configuration
public class ReportingConfig {

    /**
     * The pool reports render on. Bounded on purpose, with a bounded queue: if far more
     * reports are requested than can be produced, new requests are rejected loudly rather
     * than piling up until the service runs out of memory.
     */
    @Bean("reportExecutor")
    public TaskExecutor reportExecutor(@Value("${reporting.workers:2}") int workers) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(workers);
        executor.setMaxPoolSize(workers);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("report-");
        executor.initialize();
        return executor;
    }

    @Bean
    public Map<ReportFormat, ReportRenderer> renderersByFormat(List<ReportRenderer> renderers) {
        return renderers.stream()
            .collect(Collectors.toMap(ReportRenderer::format, Function.identity()));
    }

    @Bean
    public Map<String, ReportDataProvider> providersByCode(List<ReportDataProvider> providers) {
        return providers.stream()
            .collect(Collectors.toMap(ReportDataProvider::code, Function.identity()));
    }
}
