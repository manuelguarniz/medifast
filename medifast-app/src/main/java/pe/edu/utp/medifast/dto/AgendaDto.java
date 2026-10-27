package pe.edu.utp.medifast.dto;

import java.util.List;

import pe.edu.utp.medifast.model.HorarioSemanal;
import pe.edu.utp.medifast.model.VistaCita;

/**
 * Agenda del médico (P7, RF04). Los indicadores se calculan sobre todas las citas del período, aunque la lista
 * esté filtrada por estado.
 *
 * @param citas citas del período que cumplen el filtro de estado, por fecha y hora
 * @param total citas del período en cualquier estado
 * @param porValidar citas {@code PROGRAMADA} con el pago {@code POR_VALIDAR}
 * @param confirmadas citas {@code PROGRAMADA}
 * @param atendidas citas {@code ATENDIDA}
 * @param horario horario semanal del médico, para el rango de atención que muestran los indicadores (RN-08)
 */
public record AgendaDto(List<VistaCita> citas, long total, long porValidar, long confirmadas, long atendidas,
		List<HorarioSemanal> horario) {

	public AgendaDto {
		citas = List.copyOf(citas);
		horario = List.copyOf(horario);
	}
}
