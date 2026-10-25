package pe.edu.utp.medifast.security;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationFailureDisabledEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.authority.AuthorityUtils;

import pe.edu.utp.medifast.util.Auditoria;
import pe.edu.utp.medifast.util.Auditoria.Evento;

@ExtendWith(MockitoExtension.class)
class EventosAutenticacionTest {

	@Mock
	private Auditoria auditoria;

	@InjectMocks
	private EventosAutenticacion eventos;

	@Test
	void loginCorrectoRegistraElRolSinPrefijo() {
		var autenticado = UsernamePasswordAuthenticationToken.authenticated("45678912", null,
				AuthorityUtils.createAuthorityList("ROLE_PACIENTE", "FACTOR_PASSWORD"));

		eventos.loginCorrecto(new AuthenticationSuccessEvent(autenticado));

		verify(auditoria).registrar(Evento.LOGIN_OK, "45678912", "rol=PACIENTE");
	}

	@Test
	void loginFallidoNoDistingueDocumentoInexistenteDeContrasenaErrada() {
		var intento = UsernamePasswordAuthenticationToken.unauthenticated("11112222", "clave-errada");

		eventos.loginFallido(new AuthenticationFailureBadCredentialsEvent(intento,
				new BadCredentialsException("Credenciales inválidas")));

		verify(auditoria).registrar(Evento.LOGIN_FALLIDO, "11112222", "motivo=credenciales_invalidas");
	}

	@Test
	void loginDeCuentaInactivaRegistraElMotivo() {
		var intento = UsernamePasswordAuthenticationToken.unauthenticated("45678912", "clave");

		eventos.loginFallido(new AuthenticationFailureDisabledEvent(intento, new DisabledException("inactiva")));

		verify(auditoria).registrar(Evento.LOGIN_FALLIDO, "45678912", "motivo=cuenta_inactiva");
	}

	@Test
	void logoutRegistraAlUsuario() {
		var autenticado = UsernamePasswordAuthenticationToken.authenticated("45678912", null,
				AuthorityUtils.createAuthorityList("ROLE_PACIENTE"));

		eventos.logout(new LogoutSuccessEvent(autenticado));

		verify(auditoria).registrar(Evento.LOGOUT, "45678912", null);
	}
}
