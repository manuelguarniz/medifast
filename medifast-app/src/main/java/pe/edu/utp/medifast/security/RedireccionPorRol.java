package pe.edu.utp.medifast.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Dirige cada rol a su pantalla inicial tras autenticarse. */
@Component
public class RedireccionPorRol implements AuthenticationSuccessHandler {

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		String destino = authentication.getAuthorities().stream()
				.anyMatch(autoridad -> autoridad.getAuthority().equals("ROLE_PACIENTE"))
						? "/paciente/reservar"
						: "/medico/agenda";
		response.sendRedirect(request.getContextPath() + destino);
	}
}
