package pe.edu.utp.medifast.controller;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pe.edu.utp.medifast.dto.CitaPacienteDto;
import pe.edu.utp.medifast.dto.PagoPendienteDto;
import pe.edu.utp.medifast.exception.AccionNoPermitidaException;
import pe.edu.utp.medifast.exception.PagoInvalidoException;
import pe.edu.utp.medifast.exception.RecursoNoEncontradoException;
import pe.edu.utp.medifast.exception.ReservaVencidaException;
import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;
import pe.edu.utp.medifast.security.UsuarioActual;
import pe.edu.utp.medifast.service.ConstanciaService;
import pe.edu.utp.medifast.service.PagoService;
import pe.edu.utp.medifast.service.ReservaService;
import pe.edu.utp.medifast.util.EtiquetasEstado;

/** P3 — Pago Yape simulado (RF03, RN-04, RN-11) y P4 — Confirmación y detalle de la cita (RF15). */
@Controller
@RequestMapping("/paciente")
public class PagoController {

	private static final Logger LOG = LoggerFactory.getLogger(PagoController.class);
	private static final String VISTA_PAGO = "paciente/pago";

	private final PagoService pagoService;
	private final ReservaService reservaService;
	private final ConstanciaService constanciaService;
	private final UsuarioActual usuarioActual;
	private final String yapeNumero;

	public PagoController(PagoService pagoService, ReservaService reservaService, ConstanciaService constanciaService,
			UsuarioActual usuarioActual, @Value("${medifast.yape-numero:999 888 777}") String yapeNumero) {
		this.pagoService = pagoService;
		this.reservaService = reservaService;
		this.constanciaService = constanciaService;
		this.usuarioActual = usuarioActual;
		this.yapeNumero = yapeNumero;
	}

	/**
	 * P3: monto, QR de demostración y formulario de la constancia. Si la constancia ya se envió lleva a P4; si el
	 * plazo venció muestra el modal "Tiempo de reserva vencido"; en cualquier otro estado lleva a "Mis citas".
	 */
	@GetMapping("/pago")
	public String mostrarPago(@RequestParam Long citaId, Authentication authentication, Model model) {
		PagoPendienteDto pago = pagoService.paraPagar(citaId, usuarioActual.idPaciente(authentication));
		if (pago.pagada()) {
			return "redirect:/paciente/confirmacion?citaId=" + citaId;
		}
		if (!pago.pendiente() && !pago.vencida()) {
			return "redirect:/paciente/citas";
		}
		prepararPago(pago, model);
		return VISTA_PAGO;
	}

	/** SD-03: adjunta la constancia; la cita pasa a {@code PROGRAMADA} y el pago queda {@code POR_VALIDAR}. */
	@PostMapping("/pago")
	public String registrarPago(@RequestParam Long citaId, @RequestParam(required = false) String nroOperacion,
			@RequestParam(name = "comprobante", required = false) MultipartFile comprobante,
			Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
		Long idPaciente = usuarioActual.idPaciente(authentication);
		try {
			pagoService.registrarPago(citaId, idPaciente, nroOperacion, comprobante);
			redirectAttributes.addFlashAttribute("constanciaEnviada", true);
			return "redirect:/paciente/confirmacion?citaId=" + citaId;
		} catch (PagoInvalidoException ex) {
			return conErrores(citaId, idPaciente, nroOperacion, ex.getErrores(), model);
		} catch (ReservaVencidaException ex) {
			// La GET muestra el modal "Tiempo de reserva vencido".
			return "redirect:/paciente/pago?citaId=" + citaId;
		} catch (AccionNoPermitidaException ex) {
			return "redirect:/paciente/citas";
		} catch (IOException ex) {
			LOG.error("No se pudo guardar la constancia de la cita {}", citaId, ex);
			return conErrores(citaId, idPaciente, nroOperacion,
					Map.of("comprobante", "No se pudo guardar la constancia. Inténtalo de nuevo."), model);
		}
	}

	/** Vuelve a mostrar P3 con los errores bajo cada campo (validación en servidor, RNF04). */
	private String conErrores(Long citaId, Long idPaciente, String nroOperacion, Map<String, String> errores,
			Model model) {
		prepararPago(pagoService.paraPagar(citaId, idPaciente), model);
		model.addAttribute("errores", errores);
		model.addAttribute("nroOperacion", nroOperacion);
		return VISTA_PAGO;
	}

	private void prepararPago(PagoPendienteDto pago, Model model) {
		VistaCita cita = pago.cita();
		String medico = "Dr. " + cita.getMedico();
		model.addAttribute("citaId", cita.getIdCita());
		model.addAttribute("resumen", String.join(" · ",
				FormatoVista.diaCorto(cita.getFecha()),
				FormatoVista.rangoHoras12(cita.getHoraInicio(), cita.getHoraFin()),
				EtiquetasEstado.tipo(cita.getTipoCita()),
				medico));
		model.addAttribute("monto", FormatoVista.soles(pago.monto()));
		model.addAttribute("beneficiario", cita.getMedico());
		model.addAttribute("yapeNumero", yapeNumero);
		model.addAttribute("nombreMedico", medico);
		model.addAttribute("segundosRestantes", pago.segundosRestantes());
		model.addAttribute("vencida", pago.vencida());
		model.addAttribute("tamanoMaximo", PagoService.TAMANO_MAXIMO);
		if (!model.containsAttribute("errores")) {
			model.addAttribute("errores", Map.of());
		}
	}

	/**
	 * P4 — Confirmación y detalle de la cita (RF15). Al llegar desde P3 muestra "Cita reservada" con la alerta de
	 * éxito; desde "Mis citas", "Detalle de la cita" con la constancia y el estado real (decisión de flujo de P4).
	 * Una reserva que aún espera el pago vuelve a P3.
	 */
	@GetMapping("/confirmacion")
	public String confirmacion(@RequestParam Long citaId, Authentication authentication, Model model) {
		Long idPaciente = usuarioActual.idPaciente(authentication);
		CitaPacienteDto detalle = reservaService.cita(citaId, idPaciente);
		if (detalle.pagoPendiente()) {
			return "redirect:/paciente/pago?citaId=" + citaId;
		}
		VistaCita cita = detalle.cita();
		boolean reservada = Boolean.TRUE.equals(model.asMap().get("constanciaEnviada"))
				&& cita.getEstado() == EstadoCitaEnum.PROGRAMADA;
		model.addAttribute("reservada", reservada);
		model.addAttribute("citaId", cita.getIdCita());
		model.addAttribute("numero", FormatoVista.numeroCita(cita.getIdCita()));
		model.addAttribute("paciente", cita.getPaciente());
		model.addAttribute("nombreMedico", "Dr. " + cita.getMedico());
		model.addAttribute("tipo", EtiquetasEstado.tipo(cita.getTipoCita()) + " · "
				+ Duration.between(cita.getHoraInicio(), cita.getHoraFin()).toMinutes() + " min");
		model.addAttribute("fechaHora", FormatoVista.diaCortoConComa(cita.getFecha()) + " · "
				+ FormatoVista.rangoHoras12(cita.getHoraInicio(), cita.getHoraFin()));
		model.addAttribute("monto", FormatoVista.soles(detalle.monto()));
		model.addAttribute("etiquetaCita",
				detalle.vencida() ? EtiquetasEstado.VENCIDA : EtiquetasEstado.cita(cita.getEstado()));
		model.addAttribute("claseCita", detalle.vencida() ? "vencida" : EtiquetasEstado.claseCita(cita.getEstado()));
		model.addAttribute("etiquetaPago",
				cita.getEstadoPago() == null ? null : EtiquetasEstado.pago(cita.getEstadoPago()));
		model.addAttribute("clasePago", EtiquetasEstado.clasePago(cita.getEstadoPago()));
		model.addAttribute("motivo", cita.getEstado() == EstadoCitaEnum.CANCELADA ? cita.getObservacion() : null);
		model.addAttribute("permitePdf", detalle.permiteDetalle());
		model.addAttribute("constancia", !reservada && detalle.tieneConstancia()
				? nombreConstancia(citaId, idPaciente)
				: null);
		model.addAttribute("tieneConstancia", !reservada && detalle.tieneConstancia());
		return "paciente/confirmacion";
	}

	/** Nombre con el que se descarga la constancia; {@code null} si el archivo ya no está en disco. */
	private String nombreConstancia(Long citaId, Long idPaciente) {
		try {
			return constanciaService.paraPaciente(citaId, idPaciente).nombre();
		} catch (RecursoNoEncontradoException ex) {
			return null;
		}
	}
}
