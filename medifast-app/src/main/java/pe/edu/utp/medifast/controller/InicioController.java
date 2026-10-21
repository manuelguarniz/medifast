package pe.edu.utp.medifast.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Login (P0) e inicio según el rol. */
@Controller
public class InicioController {

	@GetMapping("/login")
	public String login() {
		return "login";
	}

	@GetMapping("/")
	public String inicio() {
		return "inicio";
	}
}
