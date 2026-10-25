package pe.edu.utp.medifast.security;

import java.util.stream.Collectors;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import pe.edu.utp.medifast.util.Auditoria;
import pe.edu.utp.medifast.util.Auditoria.Evento;

/** Lleva a la auditoría los eventos de login y logout que publica Spring Security (RNF13). */
@Component
@RequiredArgsConstructor
public class EventosAutenticacion {

	private static final String PREFIJO_ROL = "ROLE_";

	private final Auditoria auditoria;
	private final IntentosLoginService intentosLogin;

	@EventListener
	public void loginCorrecto(AuthenticationSuccessEvent evento) {
		Authentication autenticacion = evento.getAuthentication();
		intentosLogin.reiniciar(autenticacion.getName());
		auditoria.registrar(Evento.LOGIN_OK, autenticacion.getName(), "rol=" + roles(autenticacion));
	}

	@EventListener
	public void loginFallido(AbstractAuthenticationFailureEvent evento) {
		if (evento instanceof AuthenticationFailureBadCredentialsEvent) {
			intentosLogin.registrarFallo(evento.getAuthentication().getName());
		}
		auditoria.registrar(Evento.LOGIN_FALLIDO, evento.getAuthentication().getName(),
				"motivo=" + motivo(evento.getException()));
	}

	@EventListener
	public void logout(LogoutSuccessEvent evento) {
		auditoria.registrar(Evento.LOGOUT, evento.getAuthentication().getName(), null);
	}

	/** Solo los roles: Spring Security 7 agrega otras autoridades, como FACTOR_PASSWORD. */
	private static String roles(Authentication autenticacion) {
		return autenticacion.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.filter(autoridad -> autoridad.startsWith(PREFIJO_ROL))
				.map(rol -> rol.substring(PREFIJO_ROL.length()))
				.collect(Collectors.joining(","));
	}

	/** Motivo genérico: el log no distingue un documento inexistente de una contraseña errada. */
	private static String motivo(AuthenticationException excepcion) {
		if (excepcion instanceof DisabledException) {
			return "cuenta_inactiva";
		}
		if (excepcion instanceof LockedException) {
			return "cuenta_bloqueada";
		}
		return "credenciales_invalidas";
	}
}
