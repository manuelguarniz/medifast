package pe.edu.utp.medifast.dto;

import java.time.LocalDate;

import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;

/**
 * Filtros de los reportes (RF09): rango de fechas inclusivo y, solo en el de citas, un estado opcional. Los
 * mensajes son los del diseño aprobado de P9.
 *
 * @param desde primer día del rango
 * @param hasta último día del rango, igual o posterior a {@code desde}
 * @param estado estado de la cita; {@code null} = todos
 */
public record FiltroReporteDto(
        LocalDate desde,
        LocalDate hasta,
        EstadoCitaEnum estado) {

    public static final String FECHA_OBLIGATORIA = "Ingresa la fecha.";
    public static final String RANGO_INVALIDO = "La fecha final debe ser igual o posterior a la inicial.";

    public FiltroReporteDto(LocalDate desde, LocalDate hasta) {
        this(desde, hasta, null);
    }

    public FiltroReporteDto {
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException(FECHA_OBLIGATORIA);
        }
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException(RANGO_INVALIDO);
        }
    }
}
