package pe.edu.utp.medifast.controller;

import java.io.IOException;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

import pe.edu.utp.medifast.dto.AgendaDto;
import pe.edu.utp.medifast.dto.ConstanciaDto;
import pe.edu.utp.medifast.dto.DetalleCitaDto;
import pe.edu.utp.medifast.dto.FiltroAgendaDto;
import pe.edu.utp.medifast.dto.FiltroAgendaDto.Vista;
import pe.edu.utp.medifast.dto.MotivoDto;
import pe.edu.utp.medifast.exception.AccionNoPermitidaException;
import pe.edu.utp.medifast.exception.RecursoNoEncontradoException;
import pe.edu.utp.medifast.model.HorarioSemanal;
import pe.edu.utp.medifast.model.Medico;
import pe.edu.utp.medifast.model.Pago;
import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;
import pe.edu.utp.medifast.security.UsuarioActual;
import pe.edu.utp.medifast.service.ConstanciaService;
import pe.edu.utp.medifast.service.GestionCitaService;
import pe.edu.utp.medifast.util.EtiquetasEstado;
import pe.edu.utp.medifast.util.FormatoVista;
import pe.edu.utp.medifast.util.Paginacion;

/**
 * P7 — Agenda del médico: citas del día o de la semana con filtro por estado (RF04, CU07). P8 — Detalle de cita:
 * constancia, validar o rechazar el pago, atendida / no asistió y cancelar (RF08, RF12–RF14, CU08, CU09).
 * <p>
 * P8 conserva los filtros de P7 ({@code vista}, {@code fecha}, {@code estado}) para "‹ Volver a la agenda" y,
 * después de cada acción, vuelve a P8 con la alerta del resultado (decisión de diseño de P8).
 */
@Controller
@RequestMapping("/medico")
public class AgendaController {

	private static final Locale LOCALE = FormatoVista.LOCALE;
	private static final DateTimeFormatter HORA = FormatoVista.HORA;
	private static final String VISTA_DETALLE = "medico/detalle-cita";
	private static final String MODAL_RECHAZAR = "rechazar";
	private static final String MODAL_CANCELAR = "cancelar";
	private static final DateTimeFormatter TITULO_DIA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", LOCALE);

	private final GestionCitaService gestionCitaService;
	private final ConstanciaService constanciaService;
	private final UsuarioActual usuarioActual;
	private final Clock clock;

	public AgendaController(GestionCitaService gestionCitaService, ConstanciaService constanciaService,
			UsuarioActual usuarioActual, Clock clock) {
		this.gestionCitaService = gestionCitaService;
		this.constanciaService = constanciaService;
		this.usuarioActual = usuarioActual;
		this.clock = clock;
	}

	/**
	 * Por defecto abre el día de hoy con todos los estados (decisión de diseño de P7). Los parámetros inválidos
	 * no producen error: se vuelve al valor por defecto. Las citas se paginan con {@code pagina}; los indicadores y
	 * el contador de cada día cuentan todo el período.
	 */
	@GetMapping("/agenda")
	public String mostrar(@RequestParam(required = false) String vista, @RequestParam(required = false) String fecha,
			@RequestParam(required = false) String estado, @RequestParam(required = false) String pagina,
			Authentication authentication, Model model) {
		LocalDate hoy = LocalDate.now(clock);
		FiltroAgendaDto filtro = new FiltroAgendaDto(
				"semana".equalsIgnoreCase(vista) ? Vista.SEMANA : Vista.DIA,
				Objects.requireNonNullElse(Parametros.fecha(fecha), hoy),
				Parametros.estadoCita(estado));
		Medico medico = usuarioActual.medico(authentication);
		AgendaDto agenda = gestionCitaService.agenda(medico.getId(), filtro);
		boolean semana = filtro.vista() == Vista.SEMANA;
		LocalDate finSemana = semana ? finSemanaVisible(filtro.desde(), agenda) : filtro.fecha();
		int paso = semana ? 7 : 1;

		model.addAttribute("nombreMedico", FormatoVista.nombreMedico(medico));
		model.addAttribute("vista", semana ? "semana" : "dia");
		model.addAttribute("semana", semana);
		model.addAttribute("fecha", filtro.fecha());
		model.addAttribute("estado", filtro.estado() == null ? null : filtro.estado().name());
		model.addAttribute("etiquetaEstado", filtro.estado() == null ? null : EtiquetasEstado.cita(filtro.estado()));
		model.addAttribute("hoy", hoy);
		model.addAttribute("anterior", filtro.fecha().minusDays(paso));
		model.addAttribute("siguiente", filtro.fecha().plusDays(paso));
		model.addAttribute("subtitulo", semana
				? subtituloSemana(filtro.desde(), finSemana)
				: FormatoVista.fechaLarga(filtro.fecha()));
		model.addAttribute("etiquetaFecha", semana
				? etiquetaSemana(filtro.desde(), finSemana)
				: FormatoVista.fechaCorta(filtro.fecha()));
		model.addAttribute("etiquetaTotal", semana ? "Citas de la semana" : "Citas del día");
		model.addAttribute("notaTotal", semana
				? diasDeAtencion(agenda.horario())
				: rangoDeAtencion(agenda.horario(), filtro.fecha().getDayOfWeek()));
		model.addAttribute("agenda", agenda);
		Paginacion paginacion = Paginacion.de(pagina, agenda.citas().size());
		model.addAttribute("dias", agruparPorDia(paginacion.recortar(agenda.citas()), agenda.citas()));
		model.addAttribute("paginacion", paginacion);
		model.addAttribute("rango", paginacion.contador("cita", "citas"));
		model.addAttribute("cantidad", agenda.citas().size());
		model.addAttribute("estados", EtiquetasEstado.OPCIONES_CITA);
		return "medico/agenda";
	}

	/** SD-05: P8 con los datos de la cita, la constancia y las acciones que permite su estado. */
	@GetMapping("/citas/{id}")
	public String detalle(@PathVariable Long id, @RequestParam(required = false) String vista,
			@RequestParam(required = false) String fecha, @RequestParam(required = false) String estado,
			Authentication authentication, Model model) {
		prepararDetalle(id, Regreso.de(vista, fecha, estado), usuarioActual.medico(authentication), model);
		return VISTA_DETALLE;
	}

	/** RF12: abre la constancia en una pestaña nueva; solo la ve el médico de la cita. */
	@GetMapping("/citas/{id}/constancia")
	public ResponseEntity<Resource> verConstancia(@PathVariable Long id, Authentication authentication) {
		return Respuestas.constancia(constanciaService.paraMedico(id, usuarioActual.idMedico(authentication)));
	}

	/** RF13: valida el pago sin modal, porque no pide datos. */
	@PostMapping("/citas/{id}/validar")
	public String validarPago(@PathVariable Long id, @RequestParam(required = false) String vista,
			@RequestParam(required = false) String fecha, @RequestParam(required = false) String estado,
			Authentication authentication, RedirectAttributes redirect) {
		Long idMedico = usuarioActual.idMedico(authentication);
		return ejecutar(id, Regreso.de(vista, fecha, estado), redirect, "Pago validado.",
				() -> gestionCitaService.validarPago(id, idMedico));
	}

	/** RF13 y RN-13: el rechazo pide el motivo y cancela la cita. */
	@PostMapping("/citas/{id}/rechazar")
	public String rechazarPago(@PathVariable Long id, @Valid @ModelAttribute("motivo") MotivoDto motivo,
			BindingResult resultado, @RequestParam(required = false) String vista,
			@RequestParam(required = false) String fecha, @RequestParam(required = false) String estado,
			Authentication authentication, Model model, RedirectAttributes redirect) {
		Regreso regreso = Regreso.de(vista, fecha, estado);
		Medico medico = usuarioActual.medico(authentication);
		if (resultado.hasErrors()) {
			return conErrorDeMotivo(id, regreso, medico, MODAL_RECHAZAR, resultado, model);
		}
		return ejecutar(id, regreso, redirect, "Pago rechazado. La cita se canceló y el horario quedó libre.",
				() -> gestionCitaService.rechazarPago(id, medico.getId(), motivo.observacion()));
	}

	/** RF08: atendida exige el pago validado (RN-07) y que la hora de inicio haya llegado (RN-14). */
	@PostMapping("/citas/{id}/atendida")
	public String marcarAtendida(@PathVariable Long id, @RequestParam(required = false) String vista,
			@RequestParam(required = false) String fecha, @RequestParam(required = false) String estado,
			Authentication authentication, RedirectAttributes redirect) {
		Long idMedico = usuarioActual.idMedico(authentication);
		return ejecutar(id, Regreso.de(vista, fecha, estado), redirect, "Cita marcada como atendida.",
				() -> gestionCitaService.marcarAtendida(id, idMedico));
	}

	/** RF08 y RN-14: no asistió no exige el pago. */
	@PostMapping("/citas/{id}/no-asistio")
	public String marcarNoAsistio(@PathVariable Long id, @RequestParam(required = false) String vista,
			@RequestParam(required = false) String fecha, @RequestParam(required = false) String estado,
			Authentication authentication, RedirectAttributes redirect) {
		Long idMedico = usuarioActual.idMedico(authentication);
		return ejecutar(id, Regreso.de(vista, fecha, estado), redirect, "Cita marcada como no asistió.",
				() -> gestionCitaService.marcarNoAsistio(id, idMedico));
	}

	/** RF14 y RN-13: cancelar pide el motivo y libera el horario. */
	@PostMapping("/citas/{id}/cancelar")
	public String cancelar(@PathVariable Long id, @Valid @ModelAttribute("motivo") MotivoDto motivo,
			BindingResult resultado, @RequestParam(required = false) String vista,
			@RequestParam(required = false) String fecha, @RequestParam(required = false) String estado,
			Authentication authentication, Model model, RedirectAttributes redirect) {
		Regreso regreso = Regreso.de(vista, fecha, estado);
		Medico medico = usuarioActual.medico(authentication);
		if (resultado.hasErrors()) {
			return conErrorDeMotivo(id, regreso, medico, MODAL_CANCELAR, resultado, model);
		}
		return ejecutar(id, regreso, redirect, "Cita cancelada. El horario quedó libre.",
				() -> gestionCitaService.cancelar(id, medico.getId(), motivo.observacion()));
	}

	/**
	 * Ejecuta la acción y vuelve a P8 con la alerta de éxito; si el estado ya no la permite (doble envío, otra
	 * pestaña o una URL armada a mano), vuelve con el mensaje de la regla.
	 */
	private static String ejecutar(Long id, Regreso regreso, RedirectAttributes redirect, String exito,
			Runnable accion) {
		try {
			accion.run();
			redirect.addFlashAttribute("exito", exito);
		} catch (AccionNoPermitidaException e) {
			redirect.addFlashAttribute("error", e.getMessage());
		}
		redirect.addAttribute("id", id);
		regreso.agregarA(redirect);
		return "redirect:/medico/citas/{id}";
	}

	/** Vuelve a mostrar P8 con el modal abierto y el error bajo el motivo (validación en servidor, RNF04). */
	private String conErrorDeMotivo(Long id, Regreso regreso, Medico medico, String modal, BindingResult resultado,
			Model model) {
		prepararDetalle(id, regreso, medico, model);
		model.addAttribute("modalAbierto", modal);
		model.addAttribute("errorMotivo", resultado.getFieldError("observacion").getDefaultMessage());
		return VISTA_DETALLE;
	}

	private void prepararDetalle(Long id, Regreso regreso, Medico medico, Model model) {
		DetalleCitaDto detalle = gestionCitaService.detalle(id, medico.getId());
		VistaCita cita = detalle.cita();
		model.addAttribute("nombreMedico", FormatoVista.nombreMedico(medico));
		model.addAttribute("detalle", detalle);
		model.addAttribute("cita", vistaDetalle(detalle, LocalDateTime.now(clock)));
		model.addAttribute("constancia", detalle.pago() == null ? null : constancia(id, medico.getId()));
		model.addAttribute("regreso", regreso.fecha() == null ? regreso.conFecha(cita.getFecha()) : regreso);
		model.addAttribute("filtros", regreso);
		model.addAttribute("maximoMotivo", MotivoDto.MAXIMO);
		if (!model.containsAttribute("motivo")) {
			model.addAttribute("motivo", new MotivoDto(null));
		}
	}

	private static DetalleView vistaDetalle(DetalleCitaDto detalle, LocalDateTime ahora) {
		VistaCita cita = detalle.cita();
		Pago pago = detalle.pago();
		return new DetalleView(
				FormatoVista.numeroCita(cita.getIdCita()),
				FormatoVista.fechaLarga(cita.getFecha()) + " · "
						+ FormatoVista.rangoHoras12(cita.getHoraInicio(), cita.getHoraFin()),
				EtiquetasEstado.cita(cita.getEstado()),
				EtiquetasEstado.claseCita(cita.getEstado()),
				FormatoVista.iniciales(cita.getPaciente()),
				cita.getPaciente(),
				cita.getTipoDocumento() + " " + cita.getNumeroDocumento(),
				FormatoVista.tipoYDuracion(cita),
				FormatoVista.solesOGuion(detalle.monto()),
				detalle.celular(),
				detalle.correo(),
				pago == null ? null : EtiquetasEstado.pago(pago.getEstado()),
				pago == null ? null : EtiquetasEstado.clasePago(pago.getEstado()),
				pago == null ? null : StringUtils.defaultIfBlank(pago.getNroOperacion(), "—"),
				pago == null ? null : FormatoVista.fechaHora12(pago.getFechaPago()),
				pago == null || pago.getValidadoEn() == null ? null : FormatoVista.fechaHora12(pago.getValidadoEn()),
				cita.getExpiraEn() == null ? null : FormatoVista.hora12(cita.getExpiraEn().toLocalTime()),
				cita.getExpiraEn() != null && !cita.getExpiraEn().isAfter(ahora),
				cita.getEstado() == EstadoCitaEnum.CANCELADA ? cita.getObservacion() : null,
				FormatoVista.hora12(cita.getHoraInicio()),
				detalle.actualizadaEn() == null ? null : FormatoVista.fechaHora12(detalle.actualizadaEn()));
	}

	/** Nombre y tamaño de la constancia; {@code null} si falta el archivo en disco (la página se muestra igual). */
	private ConstanciaView constancia(Long idCita, Long idMedico) {
		try {
			ConstanciaDto constancia = constanciaService.paraMedico(idCita, idMedico);
			return new ConstanciaView(constancia.nombre(), tamano(constancia.archivo()),
					constancia.tipo().startsWith("image/"));
		} catch (RecursoNoEncontradoException e) {
			return null;
		}
	}

	/** "1.2 MB" o "350 KB". */
	private static String tamano(Resource archivo) {
		try {
			long bytes = archivo.contentLength();
			return bytes >= 1024 * 1024
					? String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024))
					: Math.max(1, Math.round(bytes / 1024.0)) + " KB";
		} catch (IOException e) {
			return null;
		}
	}

	/** Agrupa por día las citas de la página; {@code todas} da el total de cada día aunque esté en varias páginas. */
	private static List<DiaAgenda> agruparPorDia(List<VistaCita> pagina, List<VistaCita> todas) {
		Map<LocalDate, Long> totales = todas.stream()
				.collect(Collectors.groupingBy(VistaCita::getFecha, Collectors.counting()));
		Map<LocalDate, List<FilaAgenda>> porDia = new LinkedHashMap<>();
		for (VistaCita cita : pagina) {
			porDia.computeIfAbsent(cita.getFecha(), dia -> new ArrayList<>()).add(fila(cita));
		}
		return porDia.entrySet().stream()
				.map(dia -> new DiaAgenda(FormatoVista.capitalizar(TITULO_DIA.format(dia.getKey())), dia.getValue(),
						totales.get(dia.getKey())))
				.toList();
	}

	private static FilaAgenda fila(VistaCita cita) {
		return new FilaAgenda(
				cita.getIdCita(),
				HORA.format(cita.getHoraInicio()),
				FormatoVista.minutos(cita),
				FormatoVista.iniciales(cita.getPaciente()),
				cita.getPaciente(),
				FormatoVista.documento(cita.getTipoDocumento(), cita.getNumeroDocumento()),
				EtiquetasEstado.tipo(cita.getTipoCita()),
				EtiquetasEstado.cita(cita.getEstado()),
				EtiquetasEstado.claseCita(cita.getEstado()),
				EtiquetasEstado.pagoSiExiste(cita.getEstadoPago()),
				EtiquetasEstado.clasePago(cita.getEstadoPago()));
	}

	/**
	 * La semana se consulta de lunes a domingo, pero se rotula hasta el último día de atención (por defecto el
	 * sábado), salvo que haya citas después.
	 */
	private static LocalDate finSemanaVisible(LocalDate lunes, AgendaDto agenda) {
		DayOfWeek ultimo = agenda.horario().stream()
				.map(HorarioSemanal::getDiaSemana)
				.max(Comparator.naturalOrder())
				.orElse(DayOfWeek.SUNDAY);
		LocalDate fin = lunes.plusDays(ultimo.getValue() - 1L);
		return agenda.citas().stream()
				.map(VistaCita::getFecha)
				.filter(Objects::nonNull)
				.filter(fecha -> fecha.isAfter(fin))
				.max(Comparator.naturalOrder())
				.orElse(fin);
	}

	/** "De 09:00 a 13:00" según el horario del día (RN-08). */
	private static String rangoDeAtencion(List<HorarioSemanal> horario, DayOfWeek dia) {
		List<HorarioSemanal> delDia = horario.stream().filter(h -> h.getDiaSemana() == dia).toList();
		if (delDia.isEmpty()) {
			return "Sin horario de atención";
		}
		LocalTime inicio = delDia.stream().map(HorarioSemanal::getHoraInicio).min(Comparator.naturalOrder())
				.orElseThrow();
		LocalTime fin = delDia.stream().map(HorarioSemanal::getHoraFin).max(Comparator.naturalOrder()).orElseThrow();
		return "De " + HORA.format(inicio) + " a " + HORA.format(fin);
	}

	/** "Lunes a sábado" si los días de atención son consecutivos; si no, "Lunes, miércoles y viernes". */
	private static String diasDeAtencion(List<HorarioSemanal> horario) {
		List<DayOfWeek> dias = horario.stream().map(HorarioSemanal::getDiaSemana).distinct().sorted().toList();
		if (dias.isEmpty()) {
			return "Sin horario de atención";
		}
		if (dias.size() == 1) {
			return "Solo los " + nombreDia(dias.get(0));
		}
		boolean consecutivos = dias.get(dias.size() - 1).getValue() - dias.get(0).getValue() == dias.size() - 1;
		if (consecutivos) {
			return FormatoVista.capitalizar(nombreDia(dias.get(0))) + " a " + nombreDia(dias.get(dias.size() - 1));
		}
		List<String> nombres = dias.stream().map(AgendaController::nombreDia).toList();
		return FormatoVista.capitalizar(String.join(", ", nombres.subList(0, nombres.size() - 1)) + " y "
				+ nombres.get(nombres.size() - 1));
	}

	private static String nombreDia(DayOfWeek dia) {
		return dia.getDisplayName(TextStyle.FULL, LOCALE);
	}

	/** "Semana del 9 al 14 de noviembre de 2026"; repite el mes o el año solo si cambian. */
	private static String subtituloSemana(LocalDate desde, LocalDate hasta) {
		String fin = hasta.getDayOfMonth() + " de " + FormatoVista.mes(hasta, TextStyle.FULL) + " de " + hasta.getYear();
		String inicio = desde.getDayOfMonth()
				+ (desde.getMonth() == hasta.getMonth() ? "" : " de " + FormatoVista.mes(desde, TextStyle.FULL))
				+ (desde.getYear() == hasta.getYear() ? "" : " de " + desde.getYear());
		return "Semana del " + inicio + " al " + fin;
	}

	/** "9 – 14 nov 2026"; repite el mes o el año solo si cambian. */
	private static String etiquetaSemana(LocalDate desde, LocalDate hasta) {
		String fin = hasta.getDayOfMonth() + " " + FormatoVista.mes(hasta, TextStyle.SHORT) + " " + hasta.getYear();
		String inicio = desde.getDayOfMonth()
				+ (desde.getMonth() == hasta.getMonth() ? "" : " " + FormatoVista.mes(desde, TextStyle.SHORT))
				+ (desde.getYear() == hasta.getYear() ? "" : " " + desde.getYear());
		return inicio + " – " + fin;
	}

	/**
	 * Citas de un día en la página actual; en la vista semana, cada día lleva su cabecera con el contador.
	 * {@code total} cuenta todas las citas del día, aunque la paginación las reparta en varias páginas.
	 */
	public record DiaAgenda(String titulo, List<FilaAgenda> citas, long total) {

		public String contador() {
			return FormatoVista.cantidad(total, "cita", "citas");
		}
	}

	/**
	 * Fila de la agenda. {@code claseCita}/{@code clasePago} eligen el color del badge; {@code etiquetaPago} es
	 * {@code null} mientras la cita no tiene pago (se muestra "—").
	 */
	public record FilaAgenda(Long id, String hora, long minutos, String iniciales, String paciente, String documento,
			String tipo, String etiquetaCita, String claseCita, String etiquetaPago, String clasePago) {
	}

	/**
	 * Filtros de P7 que P8 conserva para volver a la agenda. Se normalizan como en {@link #mostrar}: lo que no es
	 * válido se descarta y la agenda usa su valor por defecto.
	 */
	public record Regreso(String vista, LocalDate fecha, String estado) {

		static Regreso de(String vista, String fecha, String estado) {
			return new Regreso(
					"semana".equalsIgnoreCase(vista) ? "semana" : "dia",
					Parametros.fecha(fecha),
					Optional.ofNullable(Parametros.estadoCita(estado)).map(Enum::name).orElse(null));
		}

		Regreso conFecha(LocalDate otra) {
			return new Regreso(vista, otra, estado);
		}

		void agregarA(RedirectAttributes redirect) {
			redirect.addAttribute("vista", vista);
			if (fecha != null) {
				redirect.addAttribute("fecha", fecha.toString());
			}
			if (estado != null) {
				redirect.addAttribute("estado", estado);
			}
		}
	}

	/**
	 * Cita lista para P8. Los datos del pago son {@code null} mientras no hay constancia; {@code motivo} solo existe
	 * en las canceladas y {@code expiraA} en las que esperan el pago.
	 */
	public record DetalleView(String numero, String fechaHora, String etiquetaCita, String claseCita,
			String iniciales, String paciente, String documento, String tipo, String monto, String celular,
			String correo, String etiquetaPago, String clasePago, String nroOperacion, String enviadaEl,
			String validadoEl, String expiraA, boolean vencida, String motivo, String horaInicio, String marcadaEl) {
	}

	/** Constancia guardada en disco: nombre para mostrar, tamaño y si se puede previsualizar como imagen. */
	public record ConstanciaView(String nombre, String tamano, boolean imagen) {
	}
}
