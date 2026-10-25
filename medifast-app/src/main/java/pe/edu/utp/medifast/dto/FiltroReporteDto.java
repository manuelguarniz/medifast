package pe.edu.utp.medifast.dto;

import java.time.LocalDate;

public record FiltroReporteDto(LocalDate desde, LocalDate hasta) {
    public FiltroReporteDto {
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Las fechas desde y hasta son obligatorias");
        }
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha desde no puede ser posterior a la fecha hasta");
        }
    }
}
