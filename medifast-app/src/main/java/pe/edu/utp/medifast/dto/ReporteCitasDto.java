package pe.edu.utp.medifast.dto;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;
import pe.edu.utp.medifast.util.Paginacion;

/**
 * Reporte de citas (P9, RF09). Los totales cuentan todas las citas del filtro, aunque {@code filas} traiga solo una
 * página.
 *
 * @param filas citas de la página, por fecha y hora
 * @param totales citas del filtro por estado
 * @param paginacion página de {@code filas} dentro del total
 */
public record ReporteCitasDto(
        List<VistaCita> filas,
        Map<EstadoCitaEnum, Long> totales,
        Paginacion paginacion) {

    /** Todas las citas del filtro en una sola página (exportación); los totales salen de esas filas. */
    public ReporteCitasDto(List<VistaCita> filas) {
        this(filas, contarEstados(filas), Paginacion.completa(filas.size()));
    }

    public ReporteCitasDto {
        filas = List.copyOf(filas);
        EnumMap<EstadoCitaEnum, Long> copiaTotales = new EnumMap<>(EstadoCitaEnum.class);
        for (EstadoCitaEnum estado : EstadoCitaEnum.values()) {
            copiaTotales.put(estado, 0L);
        }
        copiaTotales.putAll(totales);
        totales = Collections.unmodifiableMap(copiaTotales);
    }

    private static Map<EstadoCitaEnum, Long> contarEstados(List<VistaCita> filas) {
        EnumMap<EstadoCitaEnum, Long> totales = new EnumMap<>(EstadoCitaEnum.class);

        for (VistaCita cita : filas) {
            if (cita.getEstado() != null) {
                totales.merge(cita.getEstado(), 1L, Long::sum);
            }
        }

        return totales;
    }

    public long total(EstadoCitaEnum estado) {
        return totales.getOrDefault(estado, 0L);
    }

    /** Citas del filtro en cualquier estado. */
    public long total() {
        return totales.values().stream().mapToLong(Long::longValue).sum();
    }
}
