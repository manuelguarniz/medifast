package pe.edu.utp.medifast.controller;

import java.time.format.TextStyle;
import java.util.List;

import org.springframework.core.io.Resource;
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
import pe.edu.utp.medifast.dto.MisCitasDto;
import pe.edu.utp.medifast.model.Medico;
import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;
import pe.edu.utp.medifast.security.UsuarioActual;
import pe.edu.utp.medifast.service.ConfirmacionPdfService;
import pe.edu.utp.medifast.service.ConstanciaService;
import pe.edu.utp.medifast.service.ReservaService;
import pe.edu.utp.medifast.util.EtiquetasEstado;
import pe.edu.utp.medifast.util.FormatoVista;
import pe.edu.utp.medifast.util.Paginacion;

/** P5 — Mis citas: cita activa, historial, PDF de la confirmación y constancia del paciente (RF10, RF12, RF15). */
@Controller
@RequestMapping("/paciente/citas")
public class MisCitasController {

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
		model.addAttribute("nombreMedico", medico == null ? "el médico" : FormatoVista.nombreCompletoMedico(medico));
		model.addAttribute("especialidad", medico == null ? null : medico.getEspecialidad());
		return "paciente/citas";
	}

	/** SD-08: descarga la confirmación si la cita es del paciente y está confirmada o atendida. */
	@GetMapping("/{id}/pdf")
	public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id, Authentication authentication) {
		byte[] pdf = confirmacionPdfService.generar(id, usuarioActual.idPaciente(authentication));
		return Respuestas.descarga(pdf, MediaType.APPLICATION_PDF, "cita-%06d.pdf".formatted(id));
	}

	/** RF12: abre la constancia en una pestaña nueva; solo la ve su dueño. */
	@GetMapping("/{id}/constancia")
	public ResponseEntity<Resource> verConstancia(@PathVariable Long id, Authentication authentication) {
		return Respuestas.constancia(constanciaService.paraPaciente(id, usuarioActual.idPaciente(authentication)));
	}

	private static CitaView vista(CitaPacienteDto dto) {
		VistaCita cita = dto.cita();
		String diaSemana = cita.getFecha().getDayOfWeek().getDisplayName(TextStyle.FULL, FormatoVista.LOCALE);
		return new CitaView(
				cita.getIdCita(),
				FormatoVista.numeroCita(cita.getIdCita()),
				FormatoVista.mes(cita.getFecha(), TextStyle.SHORT).toUpperCase(FormatoVista.LOCALE),
				cita.getFecha().getDayOfMonth(),
				FormatoVista.capitalizar(diaSemana),
				FormatoVista.fechaCorta(cita.getFecha()),
				FormatoVista.rangoHoras12(cita.getHoraInicio(), cita.getHoraFin()),
				FormatoVista.rangoHoras24(cita.getHoraInicio(), cita.getHoraFin()),
				EtiquetasEstado.tipo(cita.getTipoCita()),
				FormatoVista.minutos(cita),
				FormatoVista.solesOGuion(dto.monto()),
				EtiquetasEstado.cita(cita.getEstado(), dto.vencida()),
				EtiquetasEstado.claseCita(cita.getEstado(), dto.vencida()),
				EtiquetasEstado.pagoSiExiste(cita.getEstadoPago()),
				EtiquetasEstado.clasePago(cita.getEstadoPago()),
				cita.getEstado() == EstadoCitaEnum.CANCELADA ? cita.getObservacion() : null,
				dto.pagoPendiente(),
				dto.segundosRestantes(),
				dto.permiteDetalle(),
				dto.tieneConstancia());
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
