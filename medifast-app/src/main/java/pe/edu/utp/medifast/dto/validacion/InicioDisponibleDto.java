package pe.edu.utp.medifast.dto.validacion;

import java.time.LocalTime;

public record InicioDisponibleDto(
    Long idHorario,
    LocalTime horaInicio,
    LocalTime horaFin
) {}