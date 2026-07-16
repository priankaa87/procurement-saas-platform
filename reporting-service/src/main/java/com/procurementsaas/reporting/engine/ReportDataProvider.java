package com.procurementsaas.reporting.engine;

import java.util.Map;

/**
 * Supplies the contents of a report.
 *
 * <p>This is the seam between the engine and the rest of the platform. A real provider
 * calls the read API of whichever service owns the data (tender, evaluation, vendor) —
 * reporting never reaches into another service's database, because that would make every
 * schema change somebody else's outage.
 */
public interface ReportDataProvider {

    /** Matches {@code ReportDefinition.providerCode}. */
    String code();

    /**
     * @param parameters filters as supplied by the caller, already validated by the engine
     *                   to the extent it can be
     */
    ReportData fetch(Map<String, String> parameters);
}
