package pe.edu.utp.medifast.util;

import java.util.List;

/**
 * Reporte independiente del formato de salida; las listas se copian para que sea inmutable.
 *
 * @param titulo título de la hoja
 * @param filtros filtros aplicados, en texto
 * @param encabezados nombres de las columnas
 * @param filas valores de cada fila
 * @param totales fila de totales
 */
public record Reporte(
        String titulo,
        String filtros,
        List<String> encabezados,
        List<List<Object>> filas,
        List<Object> totales) {

    public Reporte {
        encabezados = List.copyOf(encabezados);
        filas = filas.stream().map(List::copyOf).toList();
        totales = List.copyOf(totales);
    }
}
