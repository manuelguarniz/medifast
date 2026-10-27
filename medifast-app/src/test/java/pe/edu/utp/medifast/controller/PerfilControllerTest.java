package pe.edu.utp.medifast.controller;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pe.edu.utp.medifast.config.SecurityConfig;
import pe.edu.utp.medifast.dao.UsuarioDao;
import pe.edu.utp.medifast.dto.PerfilDto;
import pe.edu.utp.medifast.exception.ContrasenaIncorrectaException;
import pe.edu.utp.medifast.exception.CorreoDuplicadoException;
import pe.edu.utp.medifast.model.Paciente;
import pe.edu.utp.medifast.model.Usuario;
import pe.edu.utp.medifast.model.enums.TipoDocumentoEnum;
import pe.edu.utp.medifast.security.EventosAutenticacion;
import pe.edu.utp.medifast.security.IntentosLoginService;
import pe.edu.utp.medifast.security.RedireccionPorRol;
import pe.edu.utp.medifast.security.UsuarioActual;
import pe.edu.utp.medifast.security.UsuarioDetailsService;
import pe.edu.utp.medifast.service.PacienteService;
import pe.edu.utp.medifast.util.Auditoria;

@WebMvcTest(PerfilController.class)
@Import({ SecurityConfig.class, RedireccionPorRol.class, UsuarioDetailsService.class,
		IntentosLoginService.class, EventosAutenticacion.class })
class PerfilControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PacienteService pacienteService;
	@MockitoBean
	private UsuarioActual usuarioActual;
	@MockitoBean
	private UsuarioDao usuarioDao;
	@MockitoBean
	private Auditoria auditoria;

	@BeforeEach
	void configurarPaciente() {
		Usuario usuario = mock(Usuario.class);
		when(usuario.getId()).thenReturn(5L);
		when(usuario.getTipoDocumento()).thenReturn(TipoDocumentoEnum.DNI);
		when(usuario.getNumeroDocumento()).thenReturn("41235678");
		Paciente paciente = mock(Paciente.class);
		when(paciente.getId()).thenReturn(20L);
		when(paciente.getUsuario()).thenReturn(usuario);
		when(paciente.getNombres()).thenReturn("Andrea Sofía");
		when(paciente.getApellidos()).thenReturn("Salazar Pérez");
		when(paciente.getFechaNacimiento()).thenReturn(LocalDate.of(1990, 3, 14));
		when(paciente.getCelular()).thenReturn("933731536");
		when(paciente.getCorreo()).thenReturn("andrea.salazar@gmail.com");
		when(usuarioActual.paciente(any())).thenReturn(paciente);
	}

	@Test
	void muestraLosDatosGuardadosYElDocumentoEnSoloLectura() throws Exception {
		mockMvc.perform(get("/paciente/perfil").with(user("41235678").roles("PACIENTE")))
				.andExpect(status().isOk())
				.andExpect(view().name("paciente/perfil"))
				.andExpect(content().string(allOf(
						containsString("Andrea Sofía Salazar Pérez"),
						containsString(">AS<"),
						containsString("value=\"andrea.salazar@gmail.com\""),
						containsString("value=\"1990-03-14\""),
						containsString("El documento no se puede modificar."),
						containsString("id=\"numeroDocumento\" type=\"text\" value=\"41235678\" readonly"),
						not(containsString("name=\"numeroDocumento\"")),
						not(containsString("name=\"tipoDocumento\"")))));
	}

	@Test
	void soloElPacienteAccedeAMiPerfil() throws Exception {
		mockMvc.perform(get("/paciente/perfil").with(user("10000001").roles("MEDICO")))
				.andExpect(status().isForbidden());
	}

	@Test
	void guardaLosDatosYMuestraLaAlertaDeExito() throws Exception {
		mockMvc.perform(post("/paciente/perfil").with(user("41235678").roles("PACIENTE")).with(csrf())
				.param("nombres", "Andrea Sofía")
				.param("apellidos", "Salazar Pérez")
				.param("fechaNacimiento", "1990-03-14")
				.param("celular", "987654321")
				.param("correo", "andrea.salazar@gmail.com"))
				.andExpect(redirectedUrl("/paciente/perfil"))
				.andExpect(flash().attribute("datosActualizados", true));

		verify(pacienteService).actualizar(20L, new PerfilDto("Andrea Sofía", "Salazar Pérez",
				LocalDate.of(1990, 3, 14), "987654321", "andrea.salazar@gmail.com"));
	}

	@Test
	void ignoraElDocumentoAunqueSeEnvieEnElFormulario() throws Exception {
		mockMvc.perform(post("/paciente/perfil").with(user("41235678").roles("PACIENTE")).with(csrf())
				.param("numeroDocumento", "99999999")
				.param("nombres", "Andrea Sofía")
				.param("apellidos", "Salazar Pérez")
				.param("fechaNacimiento", "1990-03-14")
				.param("celular", "933731536")
				.param("correo", "andrea.salazar@gmail.com"))
				.andExpect(redirectedUrl("/paciente/perfil"));

		verify(pacienteService).actualizar(20L, new PerfilDto("Andrea Sofía", "Salazar Pérez",
				LocalDate.of(1990, 3, 14), "933731536", "andrea.salazar@gmail.com"));
	}

	@Test
	void muestraLosErroresDeValidacionEnLaCardDeDatos() throws Exception {
		mockMvc.perform(post("/paciente/perfil").with(user("41235678").roles("PACIENTE")).with(csrf())
				.param("nombres", "Andrea Sofía")
				.param("apellidos", "Salazar Pérez")
				.param("fechaNacimiento", "1990-03-14")
				.param("celular", "87654321")
				.param("correo", "andrea.salazar@gmail"))
				.andExpect(status().isOk())
				.andExpect(view().name("paciente/perfil"))
				.andExpect(model().attributeHasFieldErrors("perfil", "celular", "correo"))
				.andExpect(content().string(allOf(
						containsString("Ingresa 9 dígitos que empiecen con 9."),
						containsString("Ingresa un correo válido."),
						containsString("value=\"87654321\""))));

		verify(pacienteService, never()).actualizar(anyLong(), any());
	}

	@Test
	void rechazaCorreoDeOtroPaciente() throws Exception {
		when(pacienteService.actualizar(eq(20L), any())).thenThrow(new CorreoDuplicadoException());

		mockMvc.perform(post("/paciente/perfil").with(user("41235678").roles("PACIENTE")).with(csrf())
				.param("nombres", "Andrea Sofía")
				.param("apellidos", "Salazar Pérez")
				.param("fechaNacimiento", "1990-03-14")
				.param("celular", "933731536")
				.param("correo", "otro@gmail.com"))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("perfil", "correo", "duplicado"))
				.andExpect(content().string(containsString("Este correo ya está registrado.")));
	}

	@Test
	void cambiaLaContrasenaYMantieneLaSesion() throws Exception {
		mockMvc.perform(post("/paciente/perfil/contrasena").with(user("41235678").roles("PACIENTE")).with(csrf())
				.param("contrasenaActual", "claveActual")
				.param("contrasena", "claveNueva1")
				.param("confirmacion", "claveNueva1"))
				.andExpect(redirectedUrl("/paciente/perfil"))
				.andExpect(flash().attribute("contrasenaActualizada", true));

		verify(pacienteService).cambiarContrasena(5L, "claveActual", "claveNueva1");
	}

	@Test
	void muestraErrorSiLaContrasenaActualNoEsCorrectaYVaciaLosCampos() throws Exception {
		doThrow(new ContrasenaIncorrectaException()).when(pacienteService)
				.cambiarContrasena(5L, "otraClave", "claveNueva1");

		mockMvc.perform(post("/paciente/perfil/contrasena").with(user("41235678").roles("PACIENTE")).with(csrf())
				.param("contrasenaActual", "otraClave")
				.param("contrasena", "claveNueva1")
				.param("confirmacion", "claveNueva1"))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("cambioContrasena", "contrasenaActual", "incorrecta"))
				.andExpect(content().string(allOf(
						containsString("La contraseña actual no es correcta."),
						not(containsString("otraClave")),
						not(containsString("claveNueva1")),
						containsString("value=\"andrea.salazar@gmail.com\""))));
	}

	@Test
	void rechazaConfirmacionDistintaSinLlamarAlServicio() throws Exception {
		mockMvc.perform(post("/paciente/perfil/contrasena").with(user("41235678").roles("PACIENTE")).with(csrf())
				.param("contrasenaActual", "claveActual")
				.param("contrasena", "claveNueva1")
				.param("confirmacion", "claveNueva2"))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrors("cambioContrasena", "confirmacion"))
				.andExpect(content().string(containsString("Las contraseñas no coinciden.")));

		verify(pacienteService, never()).cambiarContrasena(anyLong(), anyString(), anyString());
	}

	@Test
	void exigeTokenCsrf() throws Exception {
		mockMvc.perform(post("/paciente/perfil/contrasena").with(user("41235678").roles("PACIENTE"))
				.param("contrasenaActual", "claveActual")
				.param("contrasena", "claveNueva1")
				.param("confirmacion", "claveNueva1"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login?expirada"));

		verify(pacienteService, never()).cambiarContrasena(anyLong(), anyString(), anyString());
	}
}
