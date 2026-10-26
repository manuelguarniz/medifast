package pe.edu.utp.medifast.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.ZoneId;
import pe.edu.utp.medifast.dto.RegistroDto;
import pe.edu.utp.medifast.exception.CorreoDuplicadoException;
import pe.edu.utp.medifast.exception.DocumentoDuplicadoException;
import pe.edu.utp.medifast.model.enums.TipoDocumentoEnum;
import pe.edu.utp.medifast.service.PacienteService;

/** Controla la pantalla de login y el inicio de la aplicación. */
@Controller
public class AuthController {

	private final PacienteService pacienteService;

	public AuthController(PacienteService pacienteService) {
		this.pacienteService = pacienteService;
	}

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

	@GetMapping("/registro")
	public String registro(Model model) {
		if (!model.containsAttribute("registro")) {
			model.addAttribute("registro", RegistroDto.vacio());
		}
		prepararFormulario(model);
		return "registro";
	}

	@PostMapping("/registro")
	public String registrar(@Valid @ModelAttribute("registro") RegistroDto registro, BindingResult errores,
			Model model, RedirectAttributes redirectAttributes) {
		if (errores.hasErrors()) {
			prepararFormulario(model);
			return "registro";
		}

		try {
			pacienteService.registrar(registro);
		} catch (DocumentoDuplicadoException ex) {
			errores.rejectValue("numeroDocumento", "duplicado", ex.getMessage());
			prepararFormulario(model);
			return "registro";
		} catch (CorreoDuplicadoException ex) {
			errores.rejectValue("correo", "duplicado", ex.getMessage());
			prepararFormulario(model);
			return "registro";
		}

		redirectAttributes.addFlashAttribute("numeroDocumento", registro.numeroDocumentoNormalizado());
		return "redirect:/login?registrado";
	}

	private static void prepararFormulario(Model model) {
		model.addAttribute("tiposDocumento", TipoDocumentoEnum.values());
		model.addAttribute("hoy", LocalDate.now(ZoneId.of("America/Lima")));
		RegistroDto registro = (RegistroDto) model.getAttribute("registro");
		model.addAttribute("maximoDocumento",
				registro.tipoDocumento() == TipoDocumentoEnum.DNI ? 8 : 12);
	}

	private static String destinoPorRol(Authentication authentication) {
		boolean paciente = authentication.getAuthorities().stream()
				.anyMatch(autoridad -> autoridad.getAuthority().equals("ROLE_PACIENTE"));
		return paciente ? "redirect:/paciente/reservar" : "redirect:/medico/agenda";
	}
}
