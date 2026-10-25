package pe.edu.utp.medifast.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Controla la pantalla de login y el inicio de la aplicación. */
@Controller
public class AuthController {

	@GetMapping("/login")
	public String login(Authentication authentication) {
		if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
			return "login";
		}
		return destinoPorRol(authentication);
	}

	@GetMapping("/")
	public String inicio(Authentication authentication) {
		return destinoPorRol(authentication);
	}

	private static String destinoPorRol(Authentication authentication) {
		boolean paciente = authentication.getAuthorities().stream()
				.anyMatch(autoridad -> autoridad.getAuthority().equals("ROLE_PACIENTE"));
		return paciente ? "redirect:/paciente/reservar" : "redirect:/medico/agenda";
	}
}
