package pe.edu.utp.medifast.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import pe.edu.utp.medifast.dao.MedicoDao;
import pe.edu.utp.medifast.dao.PacienteDao;
import pe.edu.utp.medifast.model.Medico;
import pe.edu.utp.medifast.model.Paciente;

/** Resuelve los identificadores de dominio del usuario autenticado desde el documento de su sesión. */
@Component
@RequiredArgsConstructor
public class UsuarioActual {

	private final PacienteDao pacienteDao;
	private final MedicoDao medicoDao;

	/** Devuelve el identificador del perfil paciente asociado a la autenticación. */
	public Long idPaciente(Authentication authentication) {
		return paciente(authentication).getId();
	}

	/** Devuelve el perfil paciente asociado a la autenticación. */
	public Paciente paciente(Authentication authentication) {
		return pacienteDao.findByUsuarioNumeroDocumento(authentication.getName())
				.orElseThrow(() -> new IllegalStateException("El usuario autenticado no tiene perfil de paciente"));
	}

	/** Devuelve el identificador del perfil médico asociado a la autenticación. */
	public Long idMedico(Authentication authentication) {
		return medicoDao.findByUsuarioNumeroDocumento(authentication.getName())
				.map(Medico::getId)
				.orElseThrow(() -> new IllegalStateException("El usuario autenticado no tiene perfil de médico"));
	}
}
