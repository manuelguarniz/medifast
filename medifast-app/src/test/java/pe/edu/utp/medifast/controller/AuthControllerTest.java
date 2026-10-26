package pe.edu.utp.medifast.controller;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pe.edu.utp.medifast.config.SecurityConfig;
import pe.edu.utp.medifast.dao.UsuarioDao;
import pe.edu.utp.medifast.dto.RegistroDto;
import pe.edu.utp.medifast.exception.CorreoDuplicadoException;
import pe.edu.utp.medifast.exception.DocumentoDuplicadoException;
import pe.edu.utp.medifast.model.Usuario;
import pe.edu.utp.medifast.model.enums.RolEnum;
import pe.edu.utp.medifast.model.enums.TipoDocumentoEnum;
import pe.edu.utp.medifast.security.EventosAutenticacion;
import pe.edu.utp.medifast.security.IntentosLoginService;
import pe.edu.utp.medifast.security.RedireccionPorRol;
import pe.edu.utp.medifast.security.UsuarioDetailsService;
import pe.edu.utp.medifast.service.PacienteService;
import pe.edu.utp.medifast.util.Auditoria;

@WebMvcTest(AuthController.class)
@Import({ SecurityConfig.class, RedireccionPorRol.class, UsuarioDetailsService.class,
		IntentosLoginService.class, EventosAutenticacion.class })
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private IntentosLoginService intentosLogin;

	@MockitoBean
	private UsuarioDao usuarioDao;

	@MockitoBean
	private Auditoria auditoria;

	@MockitoBean
	private PacienteService pacienteService;

	@BeforeEach
	void configurarUsuarios() {
		intentosLogin.reiniciar("10000001");
		intentosLogin.reiniciar("70000001");
		intentosLogin.reiniciar("10000002");
		intentosLogin.reiniciar("99999999");
		when(usuarioDao.findByNumeroDocumento("10000001")).thenReturn(Optional.of(usuario("10000001", RolEnum.MEDICO)));
		when(usuarioDao.findByNumeroDocumento("70000001"))
				.thenReturn(Optional.of(usuario("70000001", RolEnum.PACIENTE)));
		when(usuarioDao.findByNumeroDocumento("10000002")).thenReturn(Optional.of(usuario("10000002", RolEnum.MEDICO)));
		when(usuarioDao.findByNumeroDocumento("99999999")).thenReturn(Optional.empty());
	}

	private static Usuario usuario(String documento, RolEnum rol) {
		return new Usuario(TipoDocumentoEnum.DNI, documento,
				"$2a$10$Kp2Urj3xvFRlV6JkzAALN.03p/dIZstkzKJAbQ3ovmtqLPTkOMzDm", rol);
	}

	@Test
	void loginSinSesionMuestraLaVista() throws Exception {
		mockMvc.perform(get("/login"))
				.andExpect(status().isOk())
				.andExpect(view().name("login"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("inputmode=\"text\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("maxlength=\"12\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("autocomplete=\"username\"")));
	}

	@Test
	void registroMuestraElFormularioConDniSeleccionado() throws Exception {
		mockMvc.perform(get("/registro"))
				.andExpect(status().isOk())
				.andExpect(view().name("registro"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Datos del paciente")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"DNI\" selected")));
	}

	@Test
	void loginPrecargaElDocumentoQueLlegaComoFlashAttribute() throws Exception {
		mockMvc.perform(get("/login").flashAttr("numeroDocumento", "70000001"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"70000001\"")));
	}

	@Test
	void registroValidoRedirigeAlLoginYPrecargaElDocumentoNormalizado() throws Exception {
		mockMvc.perform(post("/registro").with(csrf()).params(parametrosRegistro(" 70000001 ")))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login?registrado"))
				.andExpect(flash().attribute("numeroDocumento", "70000001"));

		verify(pacienteService).registrar(any(RegistroDto.class));
	}

	@Test
	void registroInvalidoMuestraElErrorEnElCampoYSinRellenarContrasenas() throws Exception {
		mockMvc.perform(post("/registro").with(csrf()).params(parametrosRegistro("70000001"))
				.param("celular", "812345678"))
				.andExpect(status().isOk())
				.andExpect(view().name("registro"))
				.andExpect(model().attributeHasFieldErrors("registro", "celular"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"Ingresa 9 dígitos que empiecen con 9.")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"2000-01-01\"")))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("claveSegura"))));
	}

	@Test
	void documentoDuplicadoSeMuestraEnSuCampo() throws Exception {
		when(pacienteService.registrar(any(RegistroDto.class))).thenThrow(new DocumentoDuplicadoException());

		mockMvc.perform(post("/registro").with(csrf()).params(parametrosRegistro("70000001")))
				.andExpect(status().isOk())
				.andExpect(view().name("registro"))
				.andExpect(model().attributeHasFieldErrors("registro", "numeroDocumento"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"Este número de documento ya está registrado.")));
	}

	@Test
	void correoDuplicadoSeMuestraEnSuCampo() throws Exception {
		when(pacienteService.registrar(any(RegistroDto.class))).thenThrow(new CorreoDuplicadoException());

		mockMvc.perform(post("/registro").with(csrf()).params(parametrosRegistro("70000001")))
				.andExpect(status().isOk())
				.andExpect(view().name("registro"))
				.andExpect(model().attributeHasFieldErrors("registro", "correo"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"Este correo ya está registrado.")));
	}

	private static org.springframework.util.MultiValueMap<String, String> parametrosRegistro(String documento) {
		org.springframework.util.LinkedMultiValueMap<String, String> parametros =
				new org.springframework.util.LinkedMultiValueMap<>();
		parametros.add("tipoDocumento", "DNI");
		parametros.add("numeroDocumento", documento);
		parametros.add("nombres", "Ana");
		parametros.add("apellidos", "Pérez");
		parametros.add("fechaNacimiento", LocalDate.of(2000, 1, 1).toString());
		parametros.add("celular", "912345678");
		parametros.add("correo", "ana@example.com");
		parametros.add("contrasena", "claveSegura");
		parametros.add("confirmacion", "claveSegura");
		return parametros;
	}

	@Test
	void loginConSesionMedicoRedirigeAAgenda() throws Exception {
		mockMvc.perform(get("/login").with(user("10000001").roles("MEDICO")))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/medico/agenda"));
	}

	@Test
	void inicioConSesionPacienteRedirigeAReservar() throws Exception {
		mockMvc.perform(get("/").with(user("70000001").roles("PACIENTE")))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/paciente/reservar"));
	}

	@Test
	void muestraMensajesDeEstadoDeLaAutenticacion() throws Exception {
		mockMvc.perform(get("/login").param("bloqueado", ""))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"Cuenta bloqueada por 15 minutos por demasiados intentos fallidos.")));
		mockMvc.perform(get("/login").param("expirada", ""))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Tu sesión expiró. Vuelve a ingresar.")));
		mockMvc.perform(get("/login").param("registrado", ""))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Cuenta creada. Ya puedes iniciar sesión.")));
		mockMvc.perform(get("/login").param("salir", ""))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Sesión cerrada.")));
		mockMvc.perform(get("/login").param("error", ""))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"N.º de documento o contraseña incorrectos.")));
	}

	@Test
	void autenticaMedicoConDocumentoYContrasenaDelFormulario() throws Exception {
		mockMvc.perform(formLogin().user("10000001").password("Medifast2026")
				.userParameter("numeroDocumento").passwordParam("contrasena"))
				.andExpect(authenticated().withUsername("10000001"))
				.andExpect(redirectedUrl("/medico/agenda"));
	}

	@Test
	void autenticaPacienteConDocumentoYContrasenaDelFormulario() throws Exception {
		mockMvc.perform(formLogin().user("70000001").password("Medifast2026")
				.userParameter("numeroDocumento").passwordParam("contrasena"))
				.andExpect(authenticated().withUsername("70000001"))
				.andExpect(redirectedUrl("/paciente/reservar"));
	}

	@Test
	void credencialesIncorrectasUsanMensajeGenerico() throws Exception {
		mockMvc.perform(formLogin().user("10000001").password("incorrecta")
				.userParameter("numeroDocumento").passwordParam("contrasena"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login?error"));
	}

	@Test
	void documentoInexistenteUsaLaMismaRedireccionDeError() throws Exception {
		mockMvc.perform(formLogin().user("99999999").password("cualquier-clave")
				.userParameter("numeroDocumento").passwordParam("contrasena"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login?error"));
	}

	@Test
	void cuentaBloqueadaRedirigeAlMensajeCorrespondiente() throws Exception {
		for (int intento = 0; intento < 5; intento++) {
			mockMvc.perform(formLogin().user("10000002").password("incorrecta")
					.userParameter("numeroDocumento").passwordParam("contrasena"))
					.andExpect(status().is3xxRedirection())
					.andExpect(redirectedUrl("/login?error"));
		}

		mockMvc.perform(formLogin().user("10000002").password("Medifast2026")
				.userParameter("numeroDocumento").passwordParam("contrasena"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login?bloqueado"));
	}

	@Test
	void loginCorrectoAntesDelQuintoFalloReiniciaElContador() throws Exception {
		for (int intento = 0; intento < 4; intento++) {
			mockMvc.perform(formLogin().user("10000002").password("incorrecta")
					.userParameter("numeroDocumento").passwordParam("contrasena"))
					.andExpect(redirectedUrl("/login?error"));
		}

		mockMvc.perform(formLogin().user("10000002").password("Medifast2026")
				.userParameter("numeroDocumento").passwordParam("contrasena"))
				.andExpect(redirectedUrl("/medico/agenda"));

		for (int intento = 0; intento < 4; intento++) {
			mockMvc.perform(formLogin().user("10000002").password("incorrecta")
					.userParameter("numeroDocumento").passwordParam("contrasena"))
					.andExpect(redirectedUrl("/login?error"));
		}
		mockMvc.perform(formLogin().user("10000002").password("Medifast2026")
				.userParameter("numeroDocumento").passwordParam("contrasena"))
				.andExpect(redirectedUrl("/medico/agenda"));
	}

	@Test
	void rolPacienteNoPuedeEntrarEnRutasDeMedico() throws Exception {
		mockMvc.perform(get("/medico/agenda").with(user("70000001").roles("PACIENTE")))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/paciente/reservar").with(user("10000001").roles("MEDICO")))
				.andExpect(status().isForbidden());
	}

	@Test
	void logoutInvalidaLaSesionYEliminaLaCookie() throws Exception {
		var sesion = new org.springframework.mock.web.MockHttpSession();

		mockMvc.perform(post("/logout").with(user("70000001").roles("PACIENTE"))
				.with(csrf()).session(sesion))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login?salir"))
				.andExpect(cookie().maxAge("JSESSIONID", 0));

		org.assertj.core.api.Assertions.assertThat(sesion.isInvalid()).isTrue();
	}
}
