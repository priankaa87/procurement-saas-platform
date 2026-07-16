package com.procurementsaas.reporting.engine;

import com.procurementsaas.reporting.domain.ReportFormat;

/**
 * Turns {@link ReportData} into bytes.
 *
 * <p>One implementation per format. Adding PDF means adding a renderer, not touching the
 * engine or any provider.
 */
public interface ReportRenderer {

    ReportFormat format();

    byte[] render(ReportData data);
}
