package pe.edu.utp.medifast.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class ExcelExportadorTest {

    private final ExcelExportador exportador = new ExcelExportador();

    @Test
    void excelExportadorGeneraEncabezadosYFilas() throws Exception {
        Reporte reporte = new Reporte(
                "Reporte de citas",
                "Desde: 2026-10-01 | Hasta: 2026-10-31 | Estado: Todos",
                List.of("Fecha", "Hora", "Monto"),
                List.of(List.of(
                        LocalDate.of(2026, 10, 25),
                        LocalTime.of(9, 30),
                        new BigDecimal("80.00"))),
                List.of("Totales", "", new BigDecimal("80.00")));

        byte[] bytes = exportador.exportar(reporte);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var hoja = workbook.getSheetAt(0);

            assertThat(hoja.getRow(0).getCell(0).getStringCellValue())
                    .isEqualTo("Reporte de citas");
            assertThat(hoja.getRow(1).getCell(0).getStringCellValue())
                    .contains("Desde: 2026-10-01");
            assertThat(hoja.getRow(3).getCell(0).getStringCellValue())
                    .isEqualTo("Fecha");
            assertThat(hoja.getRow(4).getCell(0).getCellType())
                    .isEqualTo(CellType.NUMERIC);
            assertThat(hoja.getRow(4).getCell(1).getCellType())
                    .isEqualTo(CellType.NUMERIC);
            assertThat(hoja.getPaneInformation()).isNotNull();
        }
    }

    @Test
    void excelExportadorFormateaMontoComoNumero() throws Exception {
        Reporte reporte = new Reporte(
                "Reporte de citas",
                "Filtros aplicados",
                List.of("Monto"),
                List.of(List.of(new BigDecimal("150.50"))),
                List.of(new BigDecimal("150.50")));

        byte[] bytes = exportador.exportar(reporte);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var celdaMonto = workbook.getSheetAt(0).getRow(4).getCell(0);

            assertThat(celdaMonto.getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(celdaMonto.getNumericCellValue()).isEqualTo(150.50);
            assertThat(celdaMonto.getCellStyle().getDataFormatString()).contains("S/");
        }
    }

    @Test
    void excelExportadorEscribeFechaHoraYResumenBajoLosTotales() throws Exception {
        Reporte reporte = new Reporte(
                "Reporte de pagos",
                "Filtros aplicados",
                List.of("Validado el", "Monto"),
                List.of(List.of(LocalDateTime.of(2026, 11, 9, 8, 40), new BigDecimal("80.00"))),
                List.of("Total · 1 pago", new BigDecimal("80.00")),
                List.of(
                        List.of("Pagos verificados", 1L),
                        List.of("Ticket promedio", new BigDecimal("80.00"))));

        byte[] bytes = exportador.exportar(reporte);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var hoja = workbook.getSheetAt(0);
            var validadoEl = hoja.getRow(4).getCell(0);

            assertThat(validadoEl.getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(validadoEl.getCellStyle().getDataFormatString()).isEqualTo("dd/MM/yyyy HH:mm");
            assertThat(hoja.getRow(5).getCell(0).getStringCellValue()).isEqualTo("Total · 1 pago");
            assertThat(hoja.getRow(7).getCell(0).getStringCellValue()).isEqualTo("Resumen");
            assertThat(hoja.getRow(8).getCell(0).getStringCellValue()).isEqualTo("Pagos verificados");
            assertThat(hoja.getRow(8).getCell(1).getNumericCellValue()).isEqualTo(1.0);
            assertThat(hoja.getRow(9).getCell(1).getCellStyle().getDataFormatString()).contains("S/");
        }
    }
}
