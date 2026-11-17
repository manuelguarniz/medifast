package pe.edu.utp.medifast.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Exporta un {@link Reporte} a .xlsx con Apache POI (RF09).
 *
 * <p>Usa {@link SXSSFWorkbook} (streaming): solo {@value #FILAS_EN_MEMORIA} filas quedan en memoria y el resto se
 * vuelca a un temporal, así un reporte de cientos de miles de filas no satura el heap. El ancho de las columnas se
 * calcula con la longitud del texto de cada celda: {@code autoSizeColumn} mide cada celda con fuentes AWT y con
 * 100 000 filas tardaba decenas de segundos.
 */
@Component
public class ExcelExportador implements ExportadorReporte {

    private static final int FILAS_EN_MEMORIA = 100;
    /** Ancho máximo de columna, en caracteres (Excel admite hasta 255). */
    private static final int ANCHO_MAXIMO = 60;
    /** Margen sobre el texto más largo, en caracteres. */
    private static final int MARGEN = 2;

    @Override
    public byte[] exportar(Reporte reporte) {
        SXSSFWorkbook workbook = new SXSSFWorkbook(FILAS_EN_MEMORIA);
        try (workbook; ByteArrayOutputStream salida = new ByteArrayOutputStream()) {

            Sheet hoja = workbook.createSheet("Reporte");
            int columnas = Math.max(reporte.encabezados().size(), reporte.totales().size());
            for (var valores : reporte.resumen()) {
                columnas = Math.max(columnas, valores.size());
            }
            int[] anchos = new int[columnas];
            CreationHelper helper = workbook.getCreationHelper();

            CellStyle estiloTitulo = crearEstiloTitulo(workbook);
            CellStyle estiloEncabezado = crearEstiloEncabezado(workbook);
            CellStyle estiloFecha = crearEstiloFecha(workbook, helper);
            CellStyle estiloHora = crearEstiloHora(workbook, helper);
            CellStyle estiloFechaHora = crearEstiloFechaHora(workbook, helper);
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
                medir(anchos, columna, reporte.encabezados().get(columna));
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
                            estiloFechaHora,
                            estiloMoneda);
                    medir(anchos, columna, valores.get(columna));
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
                            estiloFechaHora,
                            estiloMoneda);
                    celda.setCellStyle(combinarConNegrita(workbook, celda, estiloTotales));
                    medir(anchos, columna, reporte.totales().get(columna));
                }
                numeroFila++;
            }

            // Indicadores de RF09 debajo de la tabla, separados por una fila en blanco; el rótulo va en negrita.
            if (!reporte.resumen().isEmpty()) {
                numeroFila++;
                Row filaRotulo = hoja.createRow(numeroFila++);
                Cell celdaRotulo = filaRotulo.createCell(0);
                celdaRotulo.setCellValue("Resumen");
                celdaRotulo.setCellStyle(estiloEncabezado);
                for (var valores : reporte.resumen()) {
                    Row fila = hoja.createRow(numeroFila++);
                    for (int columna = 0; columna < valores.size(); columna++) {
                        Cell celda = fila.createCell(columna);
                        escribirValor(celda, valores.get(columna), estiloFecha, estiloHora, estiloFechaHora,
                                estiloMoneda);
                        if (columna == 0) {
                            celda.setCellStyle(estiloTotales);
                        }
                        medir(anchos, columna, valores.get(columna));
                    }
                }
            }

            // El título y la fila de filtros no cuentan: desbordan sobre las celdas vacías de la derecha.
            for (int columna = 0; columna < columnas; columna++) {
                if (anchos[columna] > 0) {
                    hoja.setColumnWidth(columna, Math.min(anchos[columna] + MARGEN, ANCHO_MAXIMO) * 256);
                }
            }

            workbook.write(salida);
            return salida.toByteArray();

        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el archivo Excel", e);
        } finally {
            workbook.dispose();
        }
    }

    /** Guarda en {@code anchos} la longitud del texto que mostrará Excel para {@code valor}, si es la mayor. */
    private static void medir(int[] anchos, int columna, Object valor) {
        if (columna < anchos.length) {
            anchos[columna] = Math.max(anchos[columna], longitudVisible(valor));
        }
    }

    private static int longitudVisible(Object valor) {
        if (valor == null) {
            return 0;
        }
        if (valor instanceof LocalDateTime) {
            return "dd/MM/yyyy HH:mm".length();
        }
        if (valor instanceof LocalDate) {
            return "dd/MM/yyyy".length();
        }
        if (valor instanceof LocalTime) {
            return "HH:mm".length();
        }
        if (valor instanceof BigDecimal monto) {
            // "S/ " + separadores de miles + 2 decimales
            String entero = monto.abs().toBigInteger().toString();
            return "S/ ".length() + entero.length() + (entero.length() - 1) / 3 + 3 + (monto.signum() < 0 ? 1 : 0);
        }
        if (valor instanceof Enum<?> enumerado) {
            return enumerado.name().length();
        }
        return valor.toString().length();
    }

    private void escribirValor(
            Cell celda,
            Object valor,
            CellStyle estiloFecha,
            CellStyle estiloHora,
            CellStyle estiloFechaHora,
            CellStyle estiloMoneda) {

        if (valor == null) {
            celda.setBlank();
            return;
        }

        if (valor instanceof LocalDateTime fechaHora) {
            celda.setCellValue(fechaHora);
            celda.setCellStyle(estiloFechaHora);
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

    private CellStyle crearEstiloFechaHora(Workbook workbook, CreationHelper helper) {
        CellStyle estilo = workbook.createCellStyle();
        estilo.setDataFormat(helper.createDataFormat().getFormat("dd/MM/yyyy HH:mm"));
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
