package pe.edu.utp.medifast.service.impl;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.utp.medifast.dao.PacienteDao;
import pe.edu.utp.medifast.dao.UsuarioDao;
import pe.edu.utp.medifast.dto.RegistroDto;
import pe.edu.utp.medifast.exception.CorreoDuplicadoException;
import pe.edu.utp.medifast.exception.DocumentoDuplicadoException;
import pe.edu.utp.medifast.model.Paciente;
import pe.edu.utp.medifast.model.Usuario;
import pe.edu.utp.medifast.model.enums.RolEnum;
import pe.edu.utp.medifast.service.PacienteService;
import pe.edu.utp.medifast.util.Auditoria;
import pe.edu.utp.medifast.util.Auditoria.Evento;

/** Implementación transaccional del registro de pacientes (RF02). */
@Service
public class PacienteServiceImpl implements PacienteService {

	private final UsuarioDao usuarioDao;
	private final PacienteDao pacienteDao;
	private final PasswordEncoder passwordEncoder;
	private final Auditoria auditoria;

	public PacienteServiceImpl(UsuarioDao usuarioDao, PacienteDao pacienteDao, PasswordEncoder passwordEncoder,
			Auditoria auditoria) {
		this.usuarioDao = usuarioDao;
		this.pacienteDao = pacienteDao;
		this.passwordEncoder = passwordEncoder;
		this.auditoria = auditoria;
	}

	@Override
	@Transactional
	public Paciente registrar(RegistroDto dto) {
		String numeroDocumento = dto.numeroDocumentoNormalizado();
		String correo = StringUtils.lowerCase(StringUtils.trim(dto.correo()), Locale.ROOT);

		if (usuarioDao.existsByNumeroDocumento(numeroDocumento)) {
			throw new DocumentoDuplicadoException();
		}
		if (pacienteDao.existsByCorreoIgnoreCase(correo)) {
			throw new CorreoDuplicadoException();
		}

		String hash = passwordEncoder.encode(dto.contrasena());
		Usuario usuario = new Usuario(dto.tipoDocumento(), numeroDocumento, hash, RolEnum.PACIENTE);

		try {
			Usuario usuarioGuardado = usuarioDao.saveAndFlush(usuario);
			Paciente paciente = new Paciente(usuarioGuardado, StringUtils.normalizeSpace(dto.nombres()),
					StringUtils.normalizeSpace(dto.apellidos()), dto.fechaNacimiento(),
					StringUtils.trim(dto.celular()), correo);
			Paciente pacienteGuardado = pacienteDao.saveAndFlush(paciente);
			auditoria.registrar(Evento.REGISTRO, numeroDocumento, "registro_paciente");
			return pacienteGuardado;
		} catch (DataIntegrityViolationException ex) {
			if (contieneRestriccion(ex, "uq_usuario_documento")) {
				throw new DocumentoDuplicadoException();
			}
			if (contieneRestriccion(ex, "uq_paciente_correo")) {
				throw new CorreoDuplicadoException();
			}
			throw ex;
		}
	}

	private static boolean contieneRestriccion(DataIntegrityViolationException ex, String nombre) {
		Throwable causa = ex.getMostSpecificCause();
		String mensaje = causa.getMessage();
		return mensaje != null && mensaje.toLowerCase(Locale.ROOT).contains(nombre);
	}
}
