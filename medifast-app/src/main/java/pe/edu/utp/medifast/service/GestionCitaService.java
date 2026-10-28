package pe.edu.utp.medifast.service;

import pe.edu.utp.medifast.dto.AgendaDto;
import pe.edu.utp.medifast.dto.DetalleCitaDto;
import pe.edu.utp.medifast.dto.FiltroAgendaDto;

/**
 * Gestión de las citas por parte del médico: la agenda (P7, RF04) y el detalle con sus acciones (P8): validar o
 * rechazar el pago (RF13), marcar atendida / no asistió (RF08) y cancelar (RF14). Todas las operaciones sobre una
 * cita verifican que pertenezca al médico autenticado (RF12).
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

	/**
	 * Detalle de una cita del médico con el paciente, el pago y las acciones que permite su estado (SD-05).
	 *
	 * @param idCita cita consultada
	 * @param idMedico médico autenticado
	 * @return datos de la cita para P8
	 * @throws pe.edu.utp.medifast.exception.RecursoNoEncontradoException si la cita no existe
	 * @throws pe.edu.utp.medifast.exception.AccesoDenegadoException si la cita es de otro médico
	 */
	DetalleCitaDto detalle(Long idCita, Long idMedico);

	/**
	 * Deja el pago {@code VALIDADO} con la fecha y hora actuales (RF13).
	 *
	 * @throws pe.edu.utp.medifast.exception.AccionNoPermitidaException si el pago no está {@code POR_VALIDAR}
	 */
	void validarPago(Long idCita, Long idMedico);

	/**
	 * En una sola transacción deja el pago {@code RECHAZADO}, la cita {@code CANCELADA} con el motivo y libera sus
	 * bloques (RF13, RN-07, RN-13).
	 *
	 * @param observacion motivo obligatorio, hasta 500 caracteres
	 * @throws IllegalArgumentException si falta el motivo o supera los 500 caracteres
	 * @throws pe.edu.utp.medifast.exception.AccionNoPermitidaException si el pago no está {@code POR_VALIDAR}
	 */
	void rechazarPago(Long idCita, Long idMedico, String observacion);

	/**
	 * Marca la cita {@code ATENDIDA} (RF08).
	 *
	 * @throws pe.edu.utp.medifast.exception.AccionNoPermitidaException si no está {@code PROGRAMADA} o aún no
	 *         llega su hora de inicio (RN-14)
	 * @throws pe.edu.utp.medifast.exception.PagoNoValidadoException si el pago no está {@code VALIDADO} (RN-07)
	 */
	void marcarAtendida(Long idCita, Long idMedico);

	/**
	 * Marca la cita {@code NO_ASISTIO} (RF08); no exige el pago validado.
	 *
	 * @throws pe.edu.utp.medifast.exception.AccionNoPermitidaException si no está {@code PROGRAMADA} o aún no
	 *         llega su hora de inicio (RN-14)
	 */
	void marcarNoAsistio(Long idCita, Long idMedico);

	/**
	 * Cancela la cita con el motivo y libera sus bloques (RF14, RN-13).
	 *
	 * @param observacion motivo obligatorio, hasta 500 caracteres
	 * @throws IllegalArgumentException si falta el motivo o supera los 500 caracteres
	 * @throws pe.edu.utp.medifast.exception.AccionNoPermitidaException si la cita no está {@code PENDIENTE_PAGO}
	 *         ni {@code PROGRAMADA}
	 */
	void cancelar(Long idCita, Long idMedico, String observacion);
}
