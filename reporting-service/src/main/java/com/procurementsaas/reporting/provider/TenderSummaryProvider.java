package com.procurementsaas.reporting.provider;

import com.procurementsaas.reporting.engine.ReportData;
import com.procurementsaas.reporting.engine.ReportDataProvider;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Tender status summary.
 *
 * <p><strong>Placeholder data.</strong> A real provider issues a read call to
 * tender-service (via the gateway, with the caller's token and tenant) and maps the
 * response onto {@link ReportData}. That is deliberately not faked here: wiring an HTTP
 * client to a service that is not running would produce a provider that looks finished and
 * works nowhere.
 *
 * <p>What is real is the shape — the engine, job lifecycle, rendering, storage, and
 * download all work against this, so swapping in the HTTP call is a change to this one
 * class.
 */
@Component
public class TenderSummaryProvider implements ReportDataProvider {

    public static final String CODE = "TENDER_SUMMARY";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public ReportData fetch(Map<String, String> parameters) {
        String status = parameters.getOrDefault("status", "ALL");

        List<List<Object>> rows = List.of(
            List.of("T-2027-001", "Laptops for field offices", "AWARDED", "SUP-A",
                new BigDecimal("48500.00"), "USD", 3),
            List.of("T-2027-002", "Server refresh", "UNDER_EVALUATION", "", BigDecimal.ZERO,
                "USD", 5),
            List.of("T-2027-003", "Office furniture", "PUBLISHED", "", BigDecimal.ZERO, "USD", 0));

        return new ReportData(
            "Tender Summary (" + status + ")",
            List.of("Tender", "Title", "Status", "Awarded To", "Value", "Currency", "Bids"),
            rows);
    }
}
