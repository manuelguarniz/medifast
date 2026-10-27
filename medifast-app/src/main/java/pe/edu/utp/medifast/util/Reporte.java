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
 * @param resumen indicadores del reporte, una fila {@code [rótulo, valor, …]} por indicador (RF09)
 */
public record Reporte(
        String titulo,
        String filtros,
        List<String> encabezados,
        List<List<Object>> filas,
        List<Object> totales,
        List<List<Object>> resumen) {

    public Reporte(String titulo, String filtros, List<String> encabezados, List<List<Object>> filas,
            List<Object> totales) {
        this(titulo, filtros, encabezados, filas, totales, List.of());
    }

    public Reporte {
        encabezados = List.copyOf(encabezados);
        filas = filas.stream().map(List::copyOf).toList();
        totales = List.copyOf(totales);
        resumen = resumen.stream().map(List::copyOf).toList();
    }
}
