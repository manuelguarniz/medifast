package pe.edu.utp.medifast.service;

import pe.edu.utp.medifast.dto.PerfilDto;
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

	/**
	 * Actualiza los datos personales del paciente (RF11). El tipo y el n.º de documento no se modifican (RN-12).
	 *
	 * @param idPaciente paciente autenticado
	 * @param dto        datos validados del formulario "Mi perfil"
	 * @return paciente actualizado
	 * @throws pe.edu.utp.medifast.exception.CorreoDuplicadoException si el correo pertenece a otro paciente
	 */
	Paciente actualizar(Long idPaciente, PerfilDto dto);

	/**
	 * Cambia la contraseña del usuario tras verificar la actual (RF11, RNF03). La sesión se mantiene.
	 *
	 * @param idUsuario usuario autenticado
	 * @param actual    contraseña actual en texto plano
	 * @param nueva     contraseña nueva ya validada
	 * @throws pe.edu.utp.medifast.exception.ContrasenaIncorrectaException si la actual no coincide
	 */
	void cambiarContrasena(Long idUsuario, String actual, String nueva);
}
