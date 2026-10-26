package pe.edu.utp.medifast.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

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
import pe.edu.utp.medifast.model.Usuario;
import pe.edu.utp.medifast.model.enums.RolEnum;
import pe.edu.utp.medifast.model.enums.TipoDocumentoEnum;
import pe.edu.utp.medifast.security.EventosAutenticacion;
import pe.edu.utp.medifast.security.IntentosLoginService;
import pe.edu.utp.medifast.security.RedireccionPorRol;
import pe.edu.utp.medifast.security.UsuarioDetailsService;
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
