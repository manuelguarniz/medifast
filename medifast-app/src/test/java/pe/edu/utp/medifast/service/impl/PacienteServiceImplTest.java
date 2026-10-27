package pe.edu.utp.medifast.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import pe.edu.utp.medifast.dao.PacienteDao;
import pe.edu.utp.medifast.dao.UsuarioDao;
import pe.edu.utp.medifast.dto.PerfilDto;
import pe.edu.utp.medifast.dto.RegistroDto;
import pe.edu.utp.medifast.exception.ContrasenaIncorrectaException;
import pe.edu.utp.medifast.exception.CorreoDuplicadoException;
import pe.edu.utp.medifast.exception.DocumentoDuplicadoException;
import pe.edu.utp.medifast.model.Paciente;
import pe.edu.utp.medifast.model.Usuario;
import pe.edu.utp.medifast.model.enums.RolEnum;
import pe.edu.utp.medifast.model.enums.TipoDocumentoEnum;
import pe.edu.utp.medifast.util.Auditoria;
import pe.edu.utp.medifast.util.Auditoria.Evento;

@ExtendWith(MockitoExtension.class)
class PacienteServiceImplTest {

	@Mock
	private UsuarioDao usuarioDao;

	@Mock
	private PacienteDao pacienteDao;

	@Mock
	private Auditoria auditoria;

	private PacienteServiceImpl service;

	@BeforeEach
	void configurarServicio() {
		service = new PacienteServiceImpl(usuarioDao, pacienteDao, new BCryptPasswordEncoder(10), auditoria);
	}

	@Test
	void registraPacienteConHashBcryptYDatosNormalizados() {
		RegistroDto dto = new RegistroDto(TipoDocumentoEnum.CE, " ab123456789 ", "  Ana   María ",
				" Pérez  Ruiz ", LocalDate.of(2000, 1, 1), "912345678", " Ana.Perez@Ejemplo.COM ",
				"claveSegura", "claveSegura");
		when(usuarioDao.existsByNumeroDocumento("AB123456789")).thenReturn(false);
		when(pacienteDao.existsByCorreoIgnoreCase("ana.perez@ejemplo.com")).thenReturn(false);
		when(usuarioDao.saveAndFlush(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
		when(pacienteDao.saveAndFlush(any(Paciente.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

		Paciente paciente = service.registrar(dto);

		ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
		verify(usuarioDao).saveAndFlush(usuarioCaptor.capture());
		Usuario usuario = usuarioCaptor.getValue();
		assertThat(usuario.getTipoDocumento()).isEqualTo(TipoDocumentoEnum.CE);
		assertThat(usuario.getNumeroDocumento()).isEqualTo("AB123456789");
		assertThat(usuario.getRol()).isEqualTo(RolEnum.PACIENTE);
		assertThat(usuario.getContrasenaHash()).startsWith("$2a$10$").hasSize(60);
		assertThat(new BCryptPasswordEncoder(10).matches("claveSegura", usuario.getContrasenaHash())).isTrue();
		assertThat(paciente.getNombres()).isEqualTo("Ana María");
		assertThat(paciente.getApellidos()).isEqualTo("Pérez Ruiz");
		assertThat(paciente.getCorreo()).isEqualTo("ana.perez@ejemplo.com");
		verify(auditoria).registrar(Evento.REGISTRO, "AB123456789", "registro_paciente");
	}

	@Test
	void rechazaDocumentoExistenteAntesDeGuardar() {
		when(usuarioDao.existsByNumeroDocumento("70000001")).thenReturn(true);

		assertThatThrownBy(() -> service.registrar(dtoDni()))
				.isInstanceOf(DocumentoDuplicadoException.class)
				.hasMessage("Este número de documento ya está registrado.");
	}

	@Test
	void rechazaCorreoExistenteAntesDeGuardar() {
		when(usuarioDao.existsByNumeroDocumento("70000001")).thenReturn(false);
		when(pacienteDao.existsByCorreoIgnoreCase("ana@example.com")).thenReturn(true);

		assertThatThrownBy(() -> service.registrar(dtoDni()))
				.isInstanceOf(CorreoDuplicadoException.class)
				.hasMessage("Este correo ya está registrado.");
	}

	@Test
	void traduceColisionConcurrenteDeDocumento() {
		when(usuarioDao.existsByNumeroDocumento("70000001")).thenReturn(false);
		when(pacienteDao.existsByCorreoIgnoreCase("ana@example.com")).thenReturn(false);
		when(usuarioDao.saveAndFlush(any(Usuario.class))).thenThrow(violacionUnica("uq_usuario_documento"));

		assertThatThrownBy(() -> service.registrar(dtoDni())).isInstanceOf(DocumentoDuplicadoException.class);
	}

	@Test
	void traduceColisionConcurrenteDeCorreo() {
		when(usuarioDao.existsByNumeroDocumento("70000001")).thenReturn(false);
		when(pacienteDao.existsByCorreoIgnoreCase("ana@example.com")).thenReturn(false);
		when(usuarioDao.saveAndFlush(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
		when(pacienteDao.saveAndFlush(any(Paciente.class))).thenThrow(violacionUnica("uq_paciente_correo"));

		assertThatThrownBy(() -> service.registrar(dtoDni())).isInstanceOf(CorreoDuplicadoException.class);
	}

	@Test
	void actualizaDatosPersonalesNormalizadosSinTocarElDocumento() {
		Usuario usuario = new Usuario(TipoDocumentoEnum.DNI, "41235678", "hash", RolEnum.PACIENTE);
		Paciente paciente = new Paciente(usuario, "Andrea Sofía", "Salazar Pérez", LocalDate.of(1990, 3, 14),
				"933731536", "andrea.salazar@gmail.com");
		when(pacienteDao.findById(20L)).thenReturn(Optional.of(paciente));
		when(pacienteDao.existsByCorreoIgnoreCaseAndIdNot("andrea.nueva@gmail.com", 20L)).thenReturn(false);
		when(pacienteDao.saveAndFlush(paciente)).thenReturn(paciente);

		Paciente actualizado = service.actualizar(20L, new PerfilDto("  Andrea   Sofía ", " Salazar  Pérez ",
				LocalDate.of(1990, 3, 15), " 987654321 ", " Andrea.Nueva@Gmail.com "));

		assertThat(actualizado.getNombres()).isEqualTo("Andrea Sofía");
		assertThat(actualizado.getApellidos()).isEqualTo("Salazar Pérez");
		assertThat(actualizado.getFechaNacimiento()).isEqualTo(LocalDate.of(1990, 3, 15));
		assertThat(actualizado.getCelular()).isEqualTo("987654321");
		assertThat(actualizado.getCorreo()).isEqualTo("andrea.nueva@gmail.com");
		assertThat(actualizado.getUsuario().getNumeroDocumento()).isEqualTo("41235678");
		verify(auditoria).registrar(Evento.PERFIL, "id_paciente=20");
	}

	@Test
	void rechazaCorreoDeOtroPacienteAlActualizar() {
		Paciente paciente = new Paciente(null, "Ana", "Pérez", LocalDate.of(2000, 1, 1), "912345678",
				"ana@example.com");
		when(pacienteDao.findById(20L)).thenReturn(Optional.of(paciente));
		when(pacienteDao.existsByCorreoIgnoreCaseAndIdNot("otro@example.com", 20L)).thenReturn(true);

		assertThatThrownBy(() -> service.actualizar(20L, new PerfilDto("Ana", "Pérez", LocalDate.of(2000, 1, 1),
				"912345678", "otro@example.com")))
				.isInstanceOf(CorreoDuplicadoException.class)
				.hasMessage("Este correo ya está registrado.");
		assertThat(paciente.getCorreo()).isEqualTo("ana@example.com");
		verify(pacienteDao, never()).saveAndFlush(any(Paciente.class));
	}

	@Test
	void traduceColisionConcurrenteDeCorreoAlActualizar() {
		Paciente paciente = new Paciente(null, "Ana", "Pérez", LocalDate.of(2000, 1, 1), "912345678",
				"ana@example.com");
		when(pacienteDao.findById(20L)).thenReturn(Optional.of(paciente));
		when(pacienteDao.existsByCorreoIgnoreCaseAndIdNot("otro@example.com", 20L)).thenReturn(false);
		when(pacienteDao.saveAndFlush(paciente)).thenThrow(violacionUnica("uq_paciente_correo"));

		assertThatThrownBy(() -> service.actualizar(20L, new PerfilDto("Ana", "Pérez", LocalDate.of(2000, 1, 1),
				"912345678", "otro@example.com"))).isInstanceOf(CorreoDuplicadoException.class);
	}

	@Test
	void cambiaContrasenaConHashBcryptCuandoLaActualEsCorrecta() {
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
		Usuario usuario = new Usuario(TipoDocumentoEnum.DNI, "41235678", encoder.encode("claveActual"),
				RolEnum.PACIENTE);
		when(usuarioDao.findById(5L)).thenReturn(Optional.of(usuario));

		service.cambiarContrasena(5L, "claveActual", "claveNueva1");

		assertThat(encoder.matches("claveNueva1", usuario.getContrasenaHash())).isTrue();
		assertThat(usuario.getContrasenaHash()).startsWith("$2a$10$");
		verify(usuarioDao).save(usuario);
		verify(auditoria).registrar(Evento.CONTRASENA, "id_usuario=5");
	}

	@Test
	void rechazaCambioDeContrasenaSiLaActualNoCoincide() {
		String hash = new BCryptPasswordEncoder(10).encode("claveActual");
		Usuario usuario = new Usuario(TipoDocumentoEnum.DNI, "41235678", hash, RolEnum.PACIENTE);
		when(usuarioDao.findById(5L)).thenReturn(Optional.of(usuario));

		assertThatThrownBy(() -> service.cambiarContrasena(5L, "otraClave", "claveNueva1"))
				.isInstanceOf(ContrasenaIncorrectaException.class)
				.hasMessage("La contraseña actual no es correcta.");
		assertThat(usuario.getContrasenaHash()).isEqualTo(hash);
		verify(usuarioDao, never()).save(any(Usuario.class));
	}

	private static RegistroDto dtoDni() {
		return new RegistroDto(TipoDocumentoEnum.DNI, "70000001", "Ana", "Pérez", LocalDate.of(2000, 1, 1),
				"912345678", "ana@example.com", "claveSegura", "claveSegura");
	}

	private static DataIntegrityViolationException violacionUnica(String constraint) {
		return new DataIntegrityViolationException("duplicate", new SQLException("Duplicate key " + constraint));
	}
}
