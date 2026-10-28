package pe.edu.utp.medifast.controller;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import pe.edu.utp.medifast.dto.CambioContrasenaDto;
import pe.edu.utp.medifast.dto.PerfilDto;
import pe.edu.utp.medifast.exception.ContrasenaIncorrectaException;
import pe.edu.utp.medifast.exception.CorreoDuplicadoException;
import pe.edu.utp.medifast.model.Paciente;
import pe.edu.utp.medifast.security.UsuarioActual;
import pe.edu.utp.medifast.service.PacienteService;
import pe.edu.utp.medifast.util.FormatoVista;

/**
 * P6 — Mi perfil (RF11): dos formularios independientes, uno para los datos personales y otro para la contraseña.
 * Cada uno muestra su propia alerta de éxito o sus errores dentro de su card.
 */
@Controller
@RequestMapping("/paciente/perfil")
@RequiredArgsConstructor
public class PerfilController {

	private static final String VISTA = "paciente/perfil";

	private final PacienteService pacienteService;
	private final UsuarioActual usuarioActual;
	private final Clock clock;

	@GetMapping
	public String perfil(Authentication authentication, Model model) {
		Paciente paciente = usuarioActual.paciente(authentication);
		if (!model.containsAttribute("perfil")) {
			model.addAttribute("perfil", PerfilDto.desde(paciente));
		}
		if (!model.containsAttribute("cambioContrasena")) {
			model.addAttribute("cambioContrasena", CambioContrasenaDto.vacio());
		}
		prepararVista(model, paciente);
		return VISTA;
	}

	@PostMapping
	public String actualizarDatos(@Valid @ModelAttribute("perfil") PerfilDto perfil, BindingResult errores,
			Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		Paciente paciente = usuarioActual.paciente(authentication);
		if (!errores.hasErrors()) {
			try {
				pacienteService.actualizar(paciente.getId(), perfil);
				redirectAttributes.addFlashAttribute("datosActualizados", true);
				return "redirect:/paciente/perfil";
			} catch (CorreoDuplicadoException ex) {
				errores.rejectValue("correo", "duplicado", ex.getMessage());
			}
		}

		model.addAttribute("cambioContrasena", CambioContrasenaDto.vacio());
		prepararVista(model, paciente);
		return VISTA;
	}

	@PostMapping("/contrasena")
	public String cambiarContrasena(@Valid @ModelAttribute("cambioContrasena") CambioContrasenaDto cambio,
			BindingResult errores, Authentication authentication, Model model,
			RedirectAttributes redirectAttributes) {
		Paciente paciente = usuarioActual.paciente(authentication);
		if (!errores.hasErrors()) {
			try {
				pacienteService.cambiarContrasena(paciente.getUsuario().getId(), cambio.contrasenaActual(),
						cambio.contrasena());
				redirectAttributes.addFlashAttribute("contrasenaActualizada", true);
				return "redirect:/paciente/perfil";
			} catch (ContrasenaIncorrectaException ex) {
				errores.rejectValue("contrasenaActual", "incorrecta", ex.getMessage());
			}
		}

		model.addAttribute("perfil", PerfilDto.desde(paciente));
		prepararVista(model, paciente);
		return VISTA;
	}

	/** La cabecera de la card y el documento salen de lo guardado, no de lo que se está editando. */
	private void prepararVista(Model model, Paciente paciente) {
		model.addAttribute("nombrePaciente", paciente.getNombres() + " " + paciente.getApellidos());
		model.addAttribute("inicialesPaciente", FormatoVista.iniciales(paciente.getNombres(), paciente.getApellidos()));
		model.addAttribute("tipoDocumento", paciente.getUsuario().getTipoDocumento());
		model.addAttribute("numeroDocumento", paciente.getUsuario().getNumeroDocumento());
		model.addAttribute("hoy", LocalDate.now(clock));
	}
}
