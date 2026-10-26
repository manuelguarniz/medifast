package pe.edu.utp.medifast.dto;

import java.time.LocalTime;

/** Estado de un bloque individual para representar la grilla de horarios. */
public record BloqueHorarioDto(Long idHorario, LocalTime horaInicio, LocalTime horaFin, boolean disponible,
		boolean ocupado, boolean bloqueado) {
}
