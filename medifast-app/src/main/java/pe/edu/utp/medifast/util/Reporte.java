package pe.edu.utp.medifast.util;

import java.util.List;

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
