package pe.edu.utp.medifast.controller;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pe.edu.utp.medifast.dao.CitaDao;
import pe.edu.utp.medifast.dao.PagoDao;
import pe.edu.utp.medifast.dao.TarifaDao;
import pe.edu.utp.medifast.model.Cita;
import pe.edu.utp.medifast.model.Pago;
import pe.edu.utp.medifast.model.Tarifa;
import pe.edu.utp.medifast.model.enums.TipoCitaEnum;
import pe.edu.utp.medifast.security.UsuarioActual;
import pe.edu.utp.medifast.service.PagoService;

@Controller
@RequestMapping("/paciente")
public class PagoController {

	private static final Locale LOCALE = Locale.forLanguageTag("es-PE");
	private static final DateTimeFormatter FECHA_FORMAT = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", LOCALE);
	private static final DateTimeFormatter HORA_FORMAT = DateTimeFormatter.ofPattern("h:mm a", LOCALE);

	private final PagoService pagoService;
	private final UsuarioActual usuarioActual;
	private final CitaDao citaDao;
	private final PagoDao pagoDao;
	private final TarifaDao tarifaDao;

	public PagoController(PagoService pagoService, UsuarioActual usuarioActual, CitaDao citaDao, PagoDao pagoDao,
			TarifaDao tarifaDao) {
		this.pagoService = pagoService;
		this.usuarioActual = usuarioActual;
		this.citaDao = citaDao;
		this.pagoDao = pagoDao;
		this.tarifaDao = tarifaDao;
	}

	@GetMapping("/pago")
	public String mostrarPago(@RequestParam Long citaId, Authentication authentication, Model model,
			RedirectAttributes redirectAttributes) {
		Long idPaciente = usuarioActual.idPaciente(authentication);
		try {
			Cita cita = pagoService.obtenerCitaParaPago(citaId, idPaciente);
			Tarifa tarifa = tarifaDao.findByMedicoIdAndTipoCita(cita.getMedico().getId(), cita.getTipoCita())
				.orElseThrow(() -> new IllegalStateException("No se encontró la tarifa de la cita."));
			model.addAttribute("cita", cita);
			model.addAttribute("tarifa", tarifa);
			model.addAttribute("paciente", cita.getPaciente());
			model.addAttribute("nombrePaciente", cita.getPaciente().getNombres() + " " + cita.getPaciente().getApellidos());
			model.addAttribute("nombreSaludo", primerNombre(cita.getPaciente().getNombres()));
			model.addAttribute("inicialesPaciente", iniciales(cita.getPaciente().getNombres(), cita.getPaciente().getApellidos()));
			model.addAttribute("monto", formatearMonto(tarifa.getPrecio()));
			model.addAttribute("fecha", capitalizar(cita.getBloques().getFirst().getHorario().getFecha().format(FECHA_FORMAT)));
			model.addAttribute("hora", cita.getBloques().getFirst().getHorario().getHoraInicio().format(HORA_FORMAT)
					+ " – " + cita.getBloques().getLast().getHorario().getHoraFin().format(HORA_FORMAT));
			model.addAttribute("tipoTexto", cita.getTipoCita() == TipoCitaEnum.NUEVO ? "Paciente nuevo" : "Paciente de control");
			model.addAttribute("nombreMedico", "Dr. " + cita.getMedico().getNombres() + " " + cita.getMedico().getApellidos());
			model.addAttribute("especialidad", cita.getMedico().getEspecialidad());
			model.addAttribute("cantidadBloques", cita.getBloques().size());
			return "paciente/pago";
		} catch (IllegalArgumentException | IllegalStateException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
			return "redirect:/paciente/reservar";
		}
	}

	@PostMapping("/pago")
	public String registrarPago(@RequestParam Long citaId, @RequestParam(required = false, defaultValue = "YAPE_SIMULADO") String metodoPago,
			@RequestParam(required = false) String nroOperacion,
			@RequestParam("comprobante") MultipartFile comprobante,
			Authentication authentication, RedirectAttributes redirectAttributes) {
		Long idPaciente = usuarioActual.idPaciente(authentication);
		try {
			pagoService.registrarPago(citaId, idPaciente, metodoPago, nroOperacion, comprobante);
			return "redirect:/paciente/confirmacion?citaId=" + citaId;
		} catch (IllegalArgumentException | IllegalStateException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
			return "redirect:/paciente/pago?citaId=" + citaId;
		} catch (Exception ex) {
			redirectAttributes.addFlashAttribute("error", "No se pudo registrar el pago. Inténtalo de nuevo.");
			return "redirect:/paciente/pago?citaId=" + citaId;
		}
	}

	@GetMapping("/confirmacion")
	public String confirmacion(@RequestParam Long citaId, Authentication authentication, Model model,
			RedirectAttributes redirectAttributes) {
		Long idPaciente = usuarioActual.idPaciente(authentication);
		try {
			Cita cita = pagoService.obtenerCitaParaPago(citaId, idPaciente);
			Pago pago = pagoDao.findByCitaId(citaId).orElse(null);
			Tarifa tarifa = tarifaDao.findByMedicoIdAndTipoCita(cita.getMedico().getId(), cita.getTipoCita())
				.orElseThrow(() -> new IllegalStateException("No se encontró la tarifa de la cita."));
			model.addAttribute("cita", cita);
			model.addAttribute("pago", pago);
			model.addAttribute("tarifa", tarifa);
			model.addAttribute("paciente", cita.getPaciente());
			model.addAttribute("nombrePaciente", cita.getPaciente().getNombres() + " " + cita.getPaciente().getApellidos());
			model.addAttribute("inicialesPaciente", iniciales(cita.getPaciente().getNombres(), cita.getPaciente().getApellidos()));
			model.addAttribute("monto", formatearMonto(tarifa.getPrecio()));
			model.addAttribute("nombreMedico", "Dr. " + cita.getMedico().getNombres() + " " + cita.getMedico().getApellidos());
			model.addAttribute("fecha", capitalizar(cita.getBloques().getFirst().getHorario().getFecha().format(FECHA_FORMAT)));
			model.addAttribute("hora", cita.getBloques().getFirst().getHorario().getHoraInicio().format(HORA_FORMAT)
					+ " – " + cita.getBloques().getLast().getHorario().getHoraFin().format(HORA_FORMAT));
			model.addAttribute("tipoTexto", cita.getTipoCita() == TipoCitaEnum.NUEVO ? "Nuevo" : "Control");
			return "paciente/confirmacion";
		} catch (IllegalArgumentException | IllegalStateException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
			return "redirect:/paciente/reservar";
		}
	}

	private static String primerNombre(String nombres) {
		return nombres == null || nombres.isBlank() ? "Paciente" : nombres.trim().split("\\s+")[0];
	}

	private static String iniciales(String nombres, String apellidos) {
		String nombre = nombres == null || nombres.isBlank() ? "" : nombres.trim().split("\\s+")[0];
		String apellido = apellidos == null || apellidos.isBlank() ? "" : apellidos.trim().split("\\s+")[0];
		return (nombre.isEmpty() ? "P" : nombre.substring(0, 1))
				+ (apellido.isEmpty() ? "P" : apellido.substring(0, 1));
	}

	private static String formatearMonto(BigDecimal monto) {
		return "S/ " + String.format(LOCALE, "%.2f", monto);
	}

	private static String capitalizar(String texto) {
		return texto.isEmpty() ? texto : texto.substring(0, 1).toUpperCase(LOCALE) + texto.substring(1);
	}
}
