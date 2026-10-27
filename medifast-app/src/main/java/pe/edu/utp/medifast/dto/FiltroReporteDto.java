package pe.edu.utp.medifast.dto;

import java.time.LocalDate;

import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;

public record FiltroReporteDto(
        LocalDate desde,
        LocalDate hasta,
        EstadoCitaEnum estado) {

    public FiltroReporteDto(LocalDate desde, LocalDate hasta) {
        this(desde, hasta, null);
    }

    public FiltroReporteDto {
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Las fechas desde y hasta son obligatorias");
        }
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha desde no puede ser posterior a la fecha hasta");
        }
    }
}
