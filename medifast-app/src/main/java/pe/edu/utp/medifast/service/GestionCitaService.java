package pe.edu.utp.medifast.service;

import pe.edu.utp.medifast.dto.AgendaDto;
import pe.edu.utp.medifast.dto.FiltroAgendaDto;

/**
 * Gestión de las citas por parte del médico. Por ahora cubre la agenda (P7, RF04); validar o rechazar el pago,
 * marcar atendida / no asistió y cancelar (P8) se agregan aquí.
 */
public interface GestionCitaService {

	/**
	 * Citas del médico en el día o la semana del filtro, con los indicadores del período.
	 *
	 * @param idMedico médico autenticado
	 * @param filtro vista (día o semana), fecha y estado opcional
	 * @return citas que cumplen el filtro, ordenadas por fecha y hora, e indicadores sobre todo el período
	 * @throws NullPointerException si falta el médico o el filtro
	 */
	AgendaDto agenda(Long idMedico, FiltroAgendaDto filtro);
}
