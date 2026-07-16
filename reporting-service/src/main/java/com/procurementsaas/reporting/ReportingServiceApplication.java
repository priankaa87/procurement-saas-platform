package com.procurementsaas.reporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Reporting Service.
 *
 * <p>Turns a request for a report into a file, without making the caller wait. A
 * comparative statement across a year of tenders is not something to render inside an HTTP
 * request: the client asks, gets a job back, and collects the file when it is ready.
 *
 * <p>Rendering runs on a small bounded pool rather than the web threads, so one person
 * exporting a large report cannot make the service unresponsive for everyone else.
 */
@EnableAsync
@SpringBootApplication
public class ReportingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReportingServiceApplication.class, args);
    }
}
