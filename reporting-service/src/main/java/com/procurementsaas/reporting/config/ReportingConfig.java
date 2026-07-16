package com.procurementsaas.reporting.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Wires the report worker pool.
 *
 * <p>The format→renderer and code→provider lookups are built inside the consumers from an
 * injected {@code List} of each interface, rather than as {@code Map} beans here.
 * Injecting a {@code Map<String, X>} would trigger Spring's convention of collecting every
 * {@code X} bean keyed by <em>bean name</em> — not by our own {@code code()} — which is a
 * silent, surprising mismatch. A list is unambiguous.
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
}
