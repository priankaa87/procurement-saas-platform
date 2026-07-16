package com.procurementsaas.reporting;

import com.procurementsaas.reporting.engine.CsvReportRenderer;
import com.procurementsaas.reporting.engine.ReportData;
import com.procurementsaas.reporting.engine.XlsxReportRenderer;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit tests for the renderers — no Spring, no database. */
class ReportRendererTest {

    private final CsvReportRenderer csv = new CsvReportRenderer();
    private final XlsxReportRenderer xlsx = new XlsxReportRenderer();

    private static ReportData sample() {
        return new ReportData("Tender Summary",
            List.of("Tender", "Value", "Bids"),
            List.of(
                Arrays.asList("T-001", new BigDecimal("48500.00"), 3),
                Arrays.asList("T-002", new BigDecimal("0.00"), 0)));
    }

    // --- CSV -----------------------------------------------------------------

    @Test
    void csvWritesAHeaderThenARowPerRecord() {
        String out = new String(csv.render(sample()), StandardCharsets.UTF_8);
        List<String> lines = out.lines().toList();
        assertThat(lines).containsExactly(
            "Tender,Value,Bids",
            "T-001,48500.00,3",
            "T-002,0.00,0");
    }

    /** A supplier called "Smith, Jones & Co" must not become two columns. */
    @Test
    void csvQuotesValuesContainingCommas() {
        ReportData data = new ReportData("X", List.of("Name", "N"),
            List.of(Arrays.asList("Smith, Jones & Co", 1)));
        String out = new String(csv.render(data), StandardCharsets.UTF_8);
        assertThat(out.lines().toList()).contains("\"Smith, Jones & Co\",1");
    }

    @Test
    void csvEscapesEmbeddedQuotesByDoublingThem() {
        ReportData data = new ReportData("X", List.of("Note"),
            List.of(List.of("He said \"hi\"")));
        String out = new String(csv.render(data), StandardCharsets.UTF_8);
        assertThat(out).contains("\"He said \"\"hi\"\"\"");
    }

    @Test
    void csvRendersNullsAsBlank() {
        ReportData data = new ReportData("X", List.of("A", "B"),
            List.of(Arrays.asList("x", null)));
        String out = new String(csv.render(data), StandardCharsets.UTF_8);
        assertThat(out.lines().toList()).contains("x,");
    }

    // --- XLSX ----------------------------------------------------------------

    /** Parse the produced bytes back with POI to prove it is a real workbook. */
    @Test
    void xlsxProducesAReadableWorkbookWithHeaderAndRows() throws Exception {
        byte[] bytes = xlsx.render(sample());
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Tender");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("T-001");
            // The value went in as a number, not a string, so it will actually sum.
            assertThat(sheet.getRow(1).getCell(1).getNumericCellValue()).isEqualTo(48500.00);
            assertThat(sheet.getRow(1).getCell(2).getNumericCellValue()).isEqualTo(3.0);
            assertThat(sheet.getLastRowNum()).isEqualTo(2);   // header + 2 data rows
        }
    }

    @Test
    void xlsxTruncatesAnOverlongTitleToAValidSheetName() throws Exception {
        String longTitle = "A".repeat(60);
        byte[] bytes = xlsx.render(new ReportData(longTitle, List.of("C"), List.of()));
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getSheetName(0)).hasSizeLessThanOrEqualTo(31);
        }
    }

    // --- ReportData ----------------------------------------------------------

    @Test
    void reportDataRejectsARowThatDoesNotMatchTheColumns() {
        assertThatThrownBy(() -> new ReportData("X", List.of("A", "B"),
            List.of(List.of("only one value")))).isInstanceOf(IllegalArgumentException.class);
    }
}
