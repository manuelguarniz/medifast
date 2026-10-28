package pe.edu.utp.medifast.controller;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import pe.edu.utp.medifast.dao.MedicoDao;
import pe.edu.utp.medifast.dto.CitaPacienteDto;
import pe.edu.utp.medifast.dto.ConstanciaDto;
import pe.edu.utp.medifast.dto.MisCitasDto;
import pe.edu.utp.medifast.model.Medico;
import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;
import pe.edu.utp.medifast.security.UsuarioActual;
import pe.edu.utp.medifast.service.ConfirmacionPdfService;
import pe.edu.utp.medifast.service.ConstanciaService;
import pe.edu.utp.medifast.service.ReservaService;
import pe.edu.utp.medifast.util.EtiquetasEstado;
import pe.edu.utp.medifast.util.Paginacion;

/** P5 — Mis citas: cita activa, historial, PDF de la confirmación y constancia del paciente (RF10, RF12, RF15). */
@Controller
@RequestMapping("/paciente/citas")
public class MisCitasController {

	private static final Locale LOCALE = Locale.forLanguageTag("es-PE");
	private static final DateTimeFormatter HORA_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", LOCALE);
	private static final DateTimeFormatter FECHA_TABLA_FORMATTER = DateTimeFormatter.ofPattern("EEE d MMM yyyy", LOCALE);

	private final ReservaService reservaService;
	private final ConfirmacionPdfService confirmacionPdfService;
	private final ConstanciaService constanciaService;
	private final UsuarioActual usuarioActual;
	private final MedicoDao medicoDao;

	public MisCitasController(ReservaService reservaService, ConfirmacionPdfService confirmacionPdfService,
			ConstanciaService constanciaService, UsuarioActual usuarioActual, MedicoDao medicoDao) {
		this.reservaService = reservaService;
		this.confirmacionPdfService = confirmacionPdfService;
		this.constanciaService = constanciaService;
		this.usuarioActual = usuarioActual;
		this.medicoDao = medicoDao;
	}

	/**
	 * Abre "Próximas" si hay una cita activa; si no, "Historial" cuando tiene citas (decisión de diseño de P5).
	 * El historial se pagina con {@code pagina}.
	 */
	@GetMapping
	public String mostrar(@RequestParam(required = false) String tab, @RequestParam(required = false) String pagina,
			Authentication authentication, Model model) {
		Long idPaciente = usuarioActual.idPaciente(authentication);
		MisCitasDto citas = reservaService.historial(idPaciente);
		CitaView proxima = citas.proxima().map(MisCitasController::vista).orElse(null);
		Paginacion paginacion = Paginacion.de(pagina, citas.historial().size());
		List<CitaView> historial = paginacion.recortar(citas.historial()).stream().map(MisCitasController::vista)
				.toList();
		String pestana = switch (tab == null ? "" : tab) {
			case "proximas", "historial" -> tab;
			default -> proxima == null && !historial.isEmpty() ? "historial" : "proximas";
		};
		// La UI opera con un médico (README §3.3): la especialidad y el estado vacío salen del médico activo.
		Medico medico = citas.proxima()
				.flatMap(cita -> medicoDao.findById(cita.cita().getIdMedico()))
				.or(medicoDao::findFirstByActivoTrueOrderByIdAsc)
				.orElse(null);

		model.addAttribute("proxima", proxima);
		model.addAttribute("historial", historial);
		model.addAttribute("paginacion", paginacion);
		model.addAttribute("rango", paginacion.contador("cita", "citas"));
		model.addAttribute("pestana", pestana);
		model.addAttribute("nombreMedico", medico == null ? "el médico"
				: "Dr. " + medico.getNombres() + " " + medico.getApellidos());
		model.addAttribute("especialidad", medico == null ? null : medico.getEspecialidad());
		return "paciente/citas";
	}

	/** SD-08: descarga la confirmación si la cita es del paciente y está confirmada o atendida. */
	@GetMapping("/{id}/pdf")
	public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id, Authentication authentication) {
		byte[] pdf = confirmacionPdfService.generar(id, usuarioActual.idPaciente(authentication));
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename("cita-%06d.pdf".formatted(id)).build().toString())
				.body(pdf);
	}

	/** RF12: abre la constancia en una pestaña nueva; solo la ve su dueño. */
	@GetMapping("/{id}/constancia")
	public ResponseEntity<Resource> verConstancia(@PathVariable Long id, Authentication authentication) {
		ConstanciaDto constancia = constanciaService.paraPaciente(id, usuarioActual.idPaciente(authentication));
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(constancia.tipo()))
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.inline().filename(constancia.nombre()).build().toString())
				.body(constancia.archivo());
	}

	private static CitaView vista(CitaPacienteDto dto) {
		VistaCita cita = dto.cita();
		long minutos = Duration.between(cita.getHoraInicio(), cita.getHoraFin()).toMinutes();
		String diaSemana = cita.getFecha().getDayOfWeek().getDisplayName(TextStyle.FULL, LOCALE);
		return new CitaView(
				cita.getIdCita(),
				"#%06d".formatted(cita.getIdCita()),
				cita.getFecha().getMonth().getDisplayName(TextStyle.SHORT, LOCALE).replace(".", "").toUpperCase(LOCALE),
				cita.getFecha().getDayOfMonth(),
				capitalizar(diaSemana),
				capitalizar(cita.getFecha().format(FECHA_TABLA_FORMATTER).replace(".", "")),
				formatearRango(cita.getHoraInicio(), cita.getHoraFin()),
				HORA_FORMATTER.format(cita.getHoraInicio()) + "–" + HORA_FORMATTER.format(cita.getHoraFin()),
				EtiquetasEstado.tipo(cita.getTipoCita()),
				minutos,
				formatearMonto(dto.monto()),
				dto.vencida() ? EtiquetasEstado.VENCIDA : EtiquetasEstado.cita(cita.getEstado()),
				dto.vencida() ? "vencida" : EtiquetasEstado.claseCita(cita.getEstado()),
				cita.getEstadoPago() == null ? null : EtiquetasEstado.pago(cita.getEstadoPago()),
				EtiquetasEstado.clasePago(cita.getEstadoPago()),
				cita.getEstado() == EstadoCitaEnum.CANCELADA ? cita.getObservacion() : null,
				dto.pagoPendiente(),
				dto.segundosRestantes(),
				dto.permiteDetalle(),
				dto.tieneConstancia());
	}

	/** "10:30–11:30 a. m."; el periodo se repite solo si cambia, como en P2. */
	private static String formatearRango(LocalTime inicio, LocalTime fin) {
		return periodo(inicio).equals(periodo(fin))
				? hora12(inicio) + "–" + hora12(fin) + " " + periodo(fin)
				: hora12(inicio) + " " + periodo(inicio) + "–" + hora12(fin) + " " + periodo(fin);
	}

	private static String hora12(LocalTime hora) {
		int hora12 = hora.getHour() % 12 == 0 ? 12 : hora.getHour() % 12;
		return "%d:%02d".formatted(hora12, hora.getMinute());
	}

	private static String periodo(LocalTime hora) {
		return hora.getHour() < 12 ? "a. m." : "p. m.";
	}

	private static String formatearMonto(BigDecimal monto) {
		return monto == null ? "—" : "S/ " + String.format(Locale.ROOT, "%.2f", monto);
	}

	private static String capitalizar(String texto) {
		return texto.isEmpty() ? texto : texto.substring(0, 1).toUpperCase(LOCALE) + texto.substring(1);
	}

	/**
	 * Cita lista para P5. {@code claseEstado}/{@code clasePago} eligen el color del badge; {@code motivo} solo
	 * existe en las canceladas.
	 */
	public record CitaView(Long id, String numero, String mes, int dia, String diaSemana, String fechaTabla,
			String horaCard, String horaTabla, String tipo, long minutos, String monto, String etiquetaEstado,
			String claseEstado, String etiquetaPago, String clasePago, String motivo, boolean continuarPago,
			long segundosRestantes, boolean permiteDetalle, boolean tieneConstancia) {

		public boolean tieneAcciones() {
			return continuarPago || permiteDetalle || tieneConstancia;
		}
	}
}
