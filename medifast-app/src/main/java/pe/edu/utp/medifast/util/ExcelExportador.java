package pe.edu.utp.medifast.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalTime;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
public class ExcelExportador implements ExportadorReporte {

    @Override
    public byte[] exportar(Reporte reporte) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {

            Sheet hoja = workbook.createSheet("Reporte");
            CreationHelper helper = workbook.getCreationHelper();

            CellStyle estiloTitulo = crearEstiloTitulo(workbook);
            CellStyle estiloEncabezado = crearEstiloEncabezado(workbook);
            CellStyle estiloFecha = crearEstiloFecha(workbook, helper);
            CellStyle estiloHora = crearEstiloHora(workbook, helper);
            CellStyle estiloMoneda = crearEstiloMoneda(workbook, helper);
            CellStyle estiloTotales = crearEstiloTotales(workbook);

            Row filaTitulo = hoja.createRow(0);
            Cell celdaTitulo = filaTitulo.createCell(0);
            celdaTitulo.setCellValue(reporte.titulo());
            celdaTitulo.setCellStyle(estiloTitulo);

            Row filaFiltros = hoja.createRow(1);
            filaFiltros.createCell(0).setCellValue(reporte.filtros());

            Row filaEncabezados = hoja.createRow(3);
            for (int columna = 0; columna < reporte.encabezados().size(); columna++) {
                Cell celda = filaEncabezados.createCell(columna);
                celda.setCellValue(reporte.encabezados().get(columna));
                celda.setCellStyle(estiloEncabezado);
            }

            hoja.createFreezePane(0, 4);

            int numeroFila = 4;
            for (var valores : reporte.filas()) {
                Row fila = hoja.createRow(numeroFila++);
                for (int columna = 0; columna < valores.size(); columna++) {
                    escribirValor(
                            fila.createCell(columna),
                            valores.get(columna),
                            estiloFecha,
                            estiloHora,
                            estiloMoneda);
                }
            }

            if (!reporte.totales().isEmpty()) {
                Row filaTotales = hoja.createRow(numeroFila);
                for (int columna = 0; columna < reporte.totales().size(); columna++) {
                    Cell celda = filaTotales.createCell(columna);
                    escribirValor(
                            celda,
                            reporte.totales().get(columna),
                            estiloFecha,
                            estiloHora,
                            estiloMoneda);
                    celda.setCellStyle(combinarConNegrita(workbook, celda, estiloTotales));
                }
            }

            int columnas = Math.max(reporte.encabezados().size(), reporte.totales().size());
            for (int columna = 0; columna < columnas; columna++) {
                hoja.autoSizeColumn(columna);
            }

            workbook.write(salida);
            return salida.toByteArray();

        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el archivo Excel", e);
        }
    }

    private void escribirValor(
            Cell celda,
            Object valor,
            CellStyle estiloFecha,
            CellStyle estiloHora,
            CellStyle estiloMoneda) {

        if (valor == null) {
            celda.setBlank();
            return;
        }

        if (valor instanceof LocalDate fecha) {
            celda.setCellValue(Date.valueOf(fecha));
            celda.setCellStyle(estiloFecha);
            return;
        }

        if (valor instanceof LocalTime hora) {
            double fraccionDia = hora.toSecondOfDay() / 86400.0;
            celda.setCellValue(fraccionDia);
            celda.setCellStyle(estiloHora);
            return;
        }

        if (valor instanceof BigDecimal monto) {
            celda.setCellValue(monto.doubleValue());
            celda.setCellStyle(estiloMoneda);
            return;
        }

        if (valor instanceof Number numero) {
            celda.setCellValue(numero.doubleValue());
            return;
        }

        if (valor instanceof Enum<?> enumerado) {
            celda.setCellValue(enumerado.name());
            return;
        }

        celda.setCellValue(valor.toString());
    }

    private CellStyle crearEstiloTitulo(Workbook workbook) {
        CellStyle estilo = workbook.createCellStyle();
        Font fuente = workbook.createFont();
        fuente.setBold(true);
        fuente.setFontHeightInPoints((short) 14);
        estilo.setFont(fuente);
        return estilo;
    }

    private CellStyle crearEstiloEncabezado(Workbook workbook) {
        CellStyle estilo = workbook.createCellStyle();
        Font fuente = workbook.createFont();
        fuente.setBold(true);
        estilo.setFont(fuente);
        estilo.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return estilo;
    }

    private CellStyle crearEstiloFecha(Workbook workbook, CreationHelper helper) {
        CellStyle estilo = workbook.createCellStyle();
        estilo.setDataFormat(helper.createDataFormat().getFormat("dd/MM/yyyy"));
        return estilo;
    }

    private CellStyle crearEstiloHora(Workbook workbook, CreationHelper helper) {
        CellStyle estilo = workbook.createCellStyle();
        estilo.setDataFormat(helper.createDataFormat().getFormat("HH:mm"));
        return estilo;
    }

    private CellStyle crearEstiloMoneda(Workbook workbook, CreationHelper helper) {
        CellStyle estilo = workbook.createCellStyle();
        estilo.setDataFormat(helper.createDataFormat().getFormat("\"S/ \"#,##0.00"));
        return estilo;
    }

    private CellStyle crearEstiloTotales(Workbook workbook) {
        CellStyle estilo = workbook.createCellStyle();
        Font fuente = workbook.createFont();
        fuente.setBold(true);
        estilo.setFont(fuente);
        return estilo;
    }

    private CellStyle combinarConNegrita(Workbook workbook, Cell celda, CellStyle estiloTotales) {
        CellStyle base = celda.getCellStyle();
        CellStyle combinado = workbook.createCellStyle();
        combinado.cloneStyleFrom(base);
        combinado.setFont(workbook.getFontAt(estiloTotales.getFontIndex()));
        return combinado;
    }
}
