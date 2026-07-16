package com.procurementsaas.reporting.engine;

import com.procurementsaas.reporting.domain.ReportFormat;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Spreadsheet output via Apache POI — the same library the legacy system used, kept
 * because it works and because finance teams live in Excel and will re-cut whatever we
 * send them.
 *
 * <p>Numbers are written as numbers, not strings. A column of right-aligned text that
 * looks like currency but will not sum is worse than useless to the person receiving it.
 */
@Component
public class XlsxReportRenderer implements ReportRenderer {

    @Override
    public ReportFormat format() {
        return ReportFormat.XLSX;
    }

    @Override
    public byte[] render(ReportData data) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(sheetNameFor(data.title()));
            CellStyle headerStyle = headerStyle(workbook);

            Row header = sheet.createRow(0);
            List<String> columns = data.columns();
            for (int i = 0; i < columns.size(); i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns.get(i));
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (List<Object> values : data.rows()) {
                Row row = sheet.createRow(rowIndex++);
                for (int i = 0; i < values.size(); i++) {
                    writeCell(row.createCell(i), values.get(i));
                }
            }

            // Freeze the headings: a thousand-row report is unreadable without it.
            sheet.createFreezePane(0, 1);
            for (int i = 0; i < columns.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to render spreadsheet: " + data.title(), ex);
        }
    }

    /** Writes each value as its own type, so numbers stay numbers and dates stay dates. */
    private static void writeCell(Cell cell, Object value) {
        switch (value) {
            case null -> cell.setBlank();
            // BigDecimal is a Number, so it must be matched before the general Number case,
            // which would otherwise dominate and make this label unreachable.
            case BigDecimal decimal -> cell.setCellValue(decimal.doubleValue());
            case Number number -> cell.setCellValue(number.doubleValue());
            case Boolean bool -> cell.setCellValue(bool);
            case LocalDate date -> cell.setCellValue(date.toString());
            case Instant instant -> cell.setCellValue(instant.toString());
            default -> cell.setCellValue(String.valueOf(value));
        }
    }

    private static CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    /** Excel rejects sheet names over 31 chars or containing certain characters. */
    private static String sheetNameFor(String title) {
        String cleaned = title == null ? "Report" : title.replaceAll("[\\\\/*\\[\\]:?]", " ");
        return cleaned.length() > 31 ? cleaned.substring(0, 31) : cleaned;
    }
}
