package pe.edu.utp.medifast.dto;

import java.time.LocalTime;

/** Inicio disponible y hora de fin de toda la cita. */
public record InicioDisponibleDto(Long idHorario, LocalTime horaInicio, LocalTime horaFin) {
}
