package com.procurementsaas.reporting.engine;

import com.procurementsaas.reporting.domain.ReportFormat;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/** CSV output, for feeding another system rather than for reading. */
@Component
public class CsvReportRenderer implements ReportRenderer {

    @Override
    public ReportFormat format() {
        return ReportFormat.CSV;
    }

    @Override
    public byte[] render(ReportData data) {
        StringBuilder out = new StringBuilder();
        out.append(String.join(",", data.columns().stream().map(CsvReportRenderer::escape).toList()));
        out.append('\n');
        for (var row : data.rows()) {
            out.append(String.join(",", row.stream()
                .map(value -> escape(value == null ? "" : String.valueOf(value)))
                .toList()));
            out.append('\n');
        }
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Quotes a value if it contains anything that would otherwise break the row apart.
     * A supplier called "Smith, Jones & Co" must not silently become two columns.
     */
    private static String escape(String value) {
        boolean needsQuoting = value.contains(",") || value.contains("\"")
            || value.contains("\n") || value.contains("\r");
        if (!needsQuoting) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
