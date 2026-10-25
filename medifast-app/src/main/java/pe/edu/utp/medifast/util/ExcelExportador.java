package pe.edu.utp.medifast.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import pe.edu.utp.medifast.dto.PagoDiaDto;
import pe.edu.utp.medifast.dto.ResumenPagosDto;
import pe.edu.utp.medifast.service.ExportadorReporte;

@Component
public class ExcelExportador implements ExportadorReporte {

    @Override
    public byte[] exportarPagos(
            ResumenPagosDto resumen,
            LocalDate desde,
            LocalDate hasta) {

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = workbook.createSheet("Pagos validados");

            Row titulo = hoja.createRow(0);
            titulo.createCell(0).setCellValue("Reporte de pagos validados");

            Row periodo = hoja.createRow(1);
            periodo.createCell(0).setCellValue("Desde");
            periodo.createCell(1).setCellValue(desde.toString());
            periodo.createCell(2).setCellValue("Hasta");
            periodo.createCell(3).setCellValue(hasta.toString());

            Row resumenFila = hoja.createRow(3);

            resumenFila.createCell(0).setCellValue("Cantidad total");

            resumenFila.createCell(1).setCellValue(resumen.getCantidadTotal());

            resumenFila.createCell(2).setCellValue("Monto total");

            resumenFila.createCell(3).setCellValue(resumen.getMontoTotal().doubleValue());

            resumenFila.createCell(4).setCellValue("Ticket promedio");

            resumenFila.createCell(5).setCellValue(resumen.getTicketPromedio().doubleValue());

            Row cabecera = hoja.createRow(5);

            cabecera.createCell(0).setCellValue("Fecha");

            cabecera.createCell(1).setCellValue("Cantidad");

            cabecera.createCell(2).setCellValue("Total S/");

            CreationHelper helper = workbook.getCreationHelper();

            CellStyle fechaEstilo = workbook.createCellStyle();

            fechaEstilo.setDataFormat(
                    helper.createDataFormat().getFormat("dd/mm/yyyy"));

            int filaActual = 6;

            for (PagoDiaDto dia : resumen.getDias()) {
                Row fila = hoja.createRow(filaActual++);

                var celdaFecha = fila.createCell(0);

                celdaFecha.setCellValue(Date.valueOf(dia.getFecha()));

                celdaFecha.setCellStyle(fechaEstilo);

                fila.createCell(1).setCellValue(dia.getCantidad());

                fila.createCell(2).setCellValue(dia.getTotal().doubleValue());
            }

            for (int columna = 0; columna <= 5; columna++) {
                hoja.autoSizeColumn(columna);
            }

            workbook.write(salida);

            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el archivo Excel", e);
        }
    }
}
