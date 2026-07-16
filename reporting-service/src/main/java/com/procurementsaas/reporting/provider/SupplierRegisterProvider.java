package com.procurementsaas.reporting.provider;

import com.procurementsaas.reporting.engine.ReportData;
import com.procurementsaas.reporting.engine.ReportDataProvider;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Supplier register.
 *
 * <p>Placeholder data, as with {@link TenderSummaryProvider} — a real provider reads
 * vendor-service and enlistment-service. See that class for why this is not stubbed out
 * with an HTTP client.
 */
@Component
public class SupplierRegisterProvider implements ReportDataProvider {

    public static final String CODE = "SUPPLIER_REGISTER";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public ReportData fetch(Map<String, String> parameters) {
        String category = parameters.getOrDefault("categoryCode", "ALL");

        List<List<Object>> rows = List.of(
            List.of("SUP-A", "Acme Supplies Ltd", "ACTIVE", "IT-HARDWARE",
                LocalDate.now().plusMonths(8), true),
            List.of("SUP-B", "Smith, Jones & Co", "ACTIVE", "IT-HARDWARE",
                LocalDate.now().plusMonths(2), true),
            List.of("SUP-C", "Dodgy Ltd", "DEBARRED", "IT-HARDWARE",
                LocalDate.now().minusDays(1), false));

        return new ReportData(
            "Supplier Register (" + category + ")",
            List.of("Code", "Name", "Status", "Category", "Enlisted Until", "Eligible"),
            rows);
    }
}
