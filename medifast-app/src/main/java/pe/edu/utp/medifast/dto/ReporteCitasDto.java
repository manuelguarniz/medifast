package pe.edu.utp.medifast.dto;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;

public record ReporteCitasDto(
        List<VistaCita> filas,
        Map<EstadoCitaEnum, Long> totales) {

    public ReporteCitasDto(List<VistaCita> filas) {
        this(filas, contarEstados(filas));
    }

    public ReporteCitasDto {
        filas = List.copyOf(filas);
        EnumMap<EstadoCitaEnum, Long> copiaTotales = new EnumMap<>(EstadoCitaEnum.class);
        copiaTotales.putAll(totales);
        totales = Collections.unmodifiableMap(copiaTotales);
    }

    private static Map<EstadoCitaEnum, Long> contarEstados(List<VistaCita> filas) {
        EnumMap<EstadoCitaEnum, Long> totales = new EnumMap<>(EstadoCitaEnum.class);

        for (EstadoCitaEnum estado : EstadoCitaEnum.values()) {
            totales.put(estado, 0L);
        }

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
}
