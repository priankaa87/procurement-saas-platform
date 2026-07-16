package com.procurementsaas.reporting.engine;

import java.util.List;

/**
 * A report's contents: column headings and rows, and nothing about how it will look.
 *
 * <p>Keeping the data flat and format-free is what lets the same report render as a
 * spreadsheet or a CSV without the provider knowing or caring which.
 *
 * @param title   shown on the rendered output
 * @param columns column headings, in order
 * @param rows    values, each row aligned to {@code columns}; nulls render as blank
 */
public record ReportData(String title, List<String> columns, List<List<Object>> rows) {

    public ReportData {
        columns = List.copyOf(columns);
        rows = rows.stream().map(row -> {
            if (row.size() != columns.size()) {
                throw new IllegalArgumentException(
                    "Row has " + row.size() + " values but there are " + columns.size()
                        + " columns: " + row);
            }
            return row;
        }).toList();
    }

    public int rowCount() {
        return rows.size();
    }
}
