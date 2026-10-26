package pe.edu.utp.medifast.service;

import pe.edu.utp.medifast.dto.RegistroDto;
import pe.edu.utp.medifast.model.Paciente;

/** Operaciones de negocio de pacientes. */
public interface PacienteService {

	/**
	 * Registra atómicamente la cuenta y los datos del paciente.
	 *
	 * @param dto datos validados del formulario de registro
	 * @return paciente creado
	 */
	Paciente registrar(RegistroDto dto);
}
