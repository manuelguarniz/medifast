package pe.edu.utp.medifast.service;

import pe.edu.utp.medifast.dto.ConstanciaDto;

/** Acceso a la constancia de pago adjunta a una cita (RF12, RNF06). */
public interface ConstanciaService {

	/**
	 * Carga la constancia de una cita del paciente autenticado; nadie más que el dueño (y el médico, en P8) la ve.
	 *
	 * @param idCita cita cuyo pago se consulta
	 * @param idPaciente paciente autenticado
	 * @return archivo, tipo MIME y nombre para mostrar
	 * @throws pe.edu.utp.medifast.exception.AccesoDenegadoException si la cita no existe o es de otro paciente
	 * @throws pe.edu.utp.medifast.exception.RecursoNoEncontradoException si la cita no tiene pago o falta el archivo
	 */
	ConstanciaDto paraPaciente(Long idCita, Long idPaciente);

	/**
	 * Carga la constancia de una cita del médico autenticado (P8, SD-05).
	 *
	 * @param idCita cita cuyo pago se consulta
	 * @param idMedico médico autenticado
	 * @return archivo, tipo MIME y nombre para mostrar
	 * @throws pe.edu.utp.medifast.exception.AccesoDenegadoException si la cita no existe o es de otro médico
	 * @throws pe.edu.utp.medifast.exception.RecursoNoEncontradoException si la cita no tiene pago o falta el archivo
	 */
	ConstanciaDto paraMedico(Long idCita, Long idMedico);
}
