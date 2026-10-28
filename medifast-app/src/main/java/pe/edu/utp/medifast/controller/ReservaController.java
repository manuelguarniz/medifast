package pe.edu.utp.medifast.controller;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pe.edu.utp.medifast.dao.HorarioDao;
import pe.edu.utp.medifast.dao.MedicoDao;
import pe.edu.utp.medifast.dto.DisponibilidadDto;
import pe.edu.utp.medifast.dto.InicioDisponibleDto;
import pe.edu.utp.medifast.exception.CitaActivaException;
import pe.edu.utp.medifast.exception.FueraDeRangoException;
import pe.edu.utp.medifast.exception.HorarioNoDisponibleException;
import pe.edu.utp.medifast.model.Cita;
import pe.edu.utp.medifast.model.Horario;
import pe.edu.utp.medifast.model.Medico;
import pe.edu.utp.medifast.model.Paciente;
import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;
import pe.edu.utp.medifast.model.enums.TipoCitaEnum;
import pe.edu.utp.medifast.security.UsuarioActual;
import pe.edu.utp.medifast.service.ReservaService;
import pe.edu.utp.medifast.util.EtiquetasEstado;
import pe.edu.utp.medifast.util.FormatoVista;

/** Presentación del flujo de selección de fecha y horario (P2). */
@Controller
@RequestMapping("/paciente/reservar")
public class ReservaController {

	private static final Locale LOCALE = FormatoVista.LOCALE;
	private static final DateTimeFormatter HORA = FormatoVista.HORA;
	private static final DateTimeFormatter MES_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", LOCALE);
	private static final DateTimeFormatter FECHA_FORMATTER =
			DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", LOCALE);
	private static final DateTimeFormatter DESCRIPCION_DIA_FORMATTER =
			DateTimeFormatter.ofPattern("EEEE d MMMM", LOCALE);

	private final ReservaService reservaService;
	private final UsuarioActual usuarioActual;
	private final MedicoDao medicoDao;
	private final HorarioDao horarioDao;
	private final Clock clock;

	public ReservaController(ReservaService reservaService, UsuarioActual usuarioActual, MedicoDao medicoDao,
			HorarioDao horarioDao, Clock clock) {
		this.reservaService = reservaService;
		this.usuarioActual = usuarioActual;
		this.medicoDao = medicoDao;
		this.horarioDao = horarioDao;
		this.clock = clock;
	}

	@GetMapping
	public String mostrar(@RequestParam(required = false) LocalDate fecha,
			@RequestParam(required = false) Long horarioId, @RequestParam(required = false) String error,
			Authentication authentication, Model model) {
		Paciente paciente = usuarioActual.paciente(authentication);
		Long idPaciente = paciente.getId();
		Medico medico = medicoDao.findFirstByActivoTrueOrderByIdAsc()
				.orElseThrow(() -> new IllegalStateException("No hay un médico activo configurado."));
		Long idMedico = medico.getId();
		LocalDateTime ahora = LocalDateTime.now(clock);
		LocalDate hoy = ahora.toLocalDate();
		int diasMin = medico.getConsultorio().getDiasAnticipacionMin();
		int diasMax = medico.getConsultorio().getDiasAnticipacionMax();
		LocalDate fechaMin = hoy.plusDays(diasMin);
		LocalDate fechaMax = hoy.plusDays(diasMax);
		LocalDate fechaSeleccionada = normalizarFecha(fecha, fechaMin, fechaMax);
		DisponibilidadDto disponibilidad =
				reservaService.disponibilidad(idPaciente, idMedico, fechaSeleccionada);

		Optional<InicioDisponibleDto> inicioSeleccionado = disponibilidad.inicios().stream()
				.filter(inicio -> inicio.idHorario().equals(horarioId))
				.findFirst();
		Long idHorarioSeleccionado = inicioSeleccionado.map(InicioDisponibleDto::idHorario).orElse(null);
		LocalDate lunes = fechaSeleccionada.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		Set<LocalDate> diasConAtencion = horarioDao.findAllByMedicoIdAndFechaBetween(idMedico, lunes,
				lunes.plusDays(6)).stream()
				.filter(horario -> !horario.isBloqueado())
				.map(Horario::getFecha)
				.collect(Collectors.toSet());
		Optional<VistaCita> citaActiva = disponibilidad.citaActiva();
		boolean pagoPendiente = citaActiva
				.filter(cita -> cita.getEstado() == EstadoCitaEnum.PENDIENTE_PAGO)
				.map(VistaCita::getExpiraEn)
				.filter(expiraEn -> expiraEn.isAfter(ahora))
				.isPresent();
		// RN-09: la cita activa solo puede estar PENDIENTE_PAGO o PROGRAMADA; pendiente sin plazo es "Vencida".
		boolean vencida = !pagoPendiente && citaActiva
				.filter(cita -> cita.getEstado() == EstadoCitaEnum.PENDIENTE_PAGO)
				.isPresent();

		model.addAttribute("medico", medico);
		model.addAttribute("disponibilidad", disponibilidad);
		model.addAttribute("dias", crearDias(lunes, fechaSeleccionada, hoy, fechaMin, fechaMax, diasConAtencion));
		model.addAttribute("hayAnterior", lunes.isAfter(fechaMin));
		model.addAttribute("haySiguiente", !lunes.plusWeeks(1).isAfter(fechaMax));
		model.addAttribute("semanaAnterior", normalizarFecha(fechaSeleccionada.minusWeeks(1), fechaMin, fechaMax));
		model.addAttribute("semanaSiguiente", normalizarFecha(fechaSeleccionada.plusWeeks(1), fechaMin, fechaMax));
		model.addAttribute("mesVisible", FormatoVista.capitalizar(fechaSeleccionada.format(MES_FORMATTER)));
		model.addAttribute("fechaCompleta", FormatoVista.capitalizar(fechaSeleccionada.format(FECHA_FORMATTER)));
		model.addAttribute("fechaCorta", FormatoVista.diaCorto(fechaSeleccionada));
		model.addAttribute("ventanaDesde", describirAnticipacion(diasMin));
		model.addAttribute("ventanaHasta", describirAnticipacion(diasMax));
		model.addAttribute("nombreMedico", FormatoVista.nombreCompletoMedico(medico));
		model.addAttribute("inicialesMedico", FormatoVista.iniciales(medico.getNombres(), medico.getApellidos()));
		model.addAttribute("especialidad", medico.getEspecialidad());
		model.addAttribute("monto", FormatoVista.soles(disponibilidad.precio()));
		model.addAttribute("minutos", disponibilidad.duracionMin());
		model.addAttribute("cantidadBloques",
				(disponibilidad.duracionMin() + medico.getConsultorio().getMinutosBloque() - 1)
						/ medico.getConsultorio().getMinutosBloque());
		model.addAttribute("tipoNombre", disponibilidad.tipo() == TipoCitaEnum.NUEVO ? "Paciente nuevo" : "Paciente de control");
		model.addAttribute("tipoCorto", EtiquetasEstado.tipo(disponibilidad.tipo()));
		model.addAttribute("tipoDescripcion", disponibilidad.tipo() == TipoCitaEnum.NUEVO
				? "Primera consulta · " + disponibilidad.duracionMin() + " minutos"
				: "Consulta de control · " + disponibilidad.duracionMin() + " minutos");
		model.addAttribute("slots", crearSlots(disponibilidad, inicioSeleccionado));
		model.addAttribute("horarioId", idHorarioSeleccionado);
		model.addAttribute("resumenHora", inicioSeleccionado
				.map(inicio -> FormatoVista.rangoHoras12Espaciado(inicio.horaInicio(), inicio.horaFin()))
				.orElse("—"));
		model.addAttribute("resumenMovil", FormatoVista.diaCorto(fechaSeleccionada) + " · " + inicioSeleccionado
				.map(inicio -> FormatoVista.rangoHoras24(inicio.horaInicio(), inicio.horaFin()))
				.orElse("—"));
		model.addAttribute("errorHorario", "horario".equals(error));
		model.addAttribute("errorFecha", "fecha".equals(error));
		model.addAttribute("citaActiva", citaActiva.orElse(null));
		model.addAttribute("pagoPendiente", pagoPendiente);
		model.addAttribute("segundosRestantes", pagoPendiente
				? Duration.between(ahora, citaActiva.get().getExpiraEn()).toSeconds()
				: 0);
		model.addAttribute("estadoActivo", citaActiva
				.map(cita -> EtiquetasEstado.claseCita(cita.getEstado(), vencida)).orElse(""));
		model.addAttribute("etiquetaEstadoCita", citaActiva
				.map(cita -> EtiquetasEstado.cita(cita.getEstado(), vencida)).orElse(""));
		model.addAttribute("fechaActiva", citaActiva.map(VistaCita::getFecha)
				.map(FormatoVista::diaCorto).orElse(""));
		model.addAttribute("rangoActivo", citaActiva
				.map(cita -> FormatoVista.rangoHoras12Espaciado(cita.getHoraInicio(), cita.getHoraFin())).orElse(""));
		model.addAttribute("tipoActivo", citaActiva.map(VistaCita::getTipoCita).map(EtiquetasEstado::tipo).orElse(""));
		model.addAttribute("mensajeError", error);
		return "paciente/reservar";
	}

	@PostMapping
	public String reservar(@RequestParam LocalDate fecha, @RequestParam Long idHorario, Authentication authentication,
			RedirectAttributes redirectAttributes) {
		Long idPaciente = usuarioActual.idPaciente(authentication);
		try {
			Cita cita = reservaService.reservar(idPaciente, idHorario);
			return "redirect:/paciente/pago?citaId=" + cita.getId();
		} catch (CitaActivaException ex) {
			redirectAttributes.addAttribute("fecha", fecha.toString());
			redirectAttributes.addAttribute("error", "activa");
			return "redirect:/paciente/reservar";
		} catch (HorarioNoDisponibleException ex) {
			redirectAttributes.addAttribute("fecha", fecha.toString());
			redirectAttributes.addAttribute("error", "horario");
			return "redirect:/paciente/reservar";
		} catch (FueraDeRangoException ex) {
			redirectAttributes.addAttribute("error", "fecha");
			return "redirect:/paciente/reservar";
		}
	}

	private static LocalDate normalizarFecha(LocalDate solicitada, LocalDate fechaMin, LocalDate fechaMax) {
		LocalDate base = solicitada == null ? fechaMin : solicitada;
		if (base.isBefore(fechaMin)) {
			return fechaMin;
		}
		if (base.isAfter(fechaMax)) {
			return fechaMax;
		}
		return base;
	}

	/** Semana visible: solo se habilitan los días dentro de la ventana con bloques sin bloquear en la BD (RN-08). */
	private static List<DiaView> crearDias(LocalDate lunes, LocalDate fechaSeleccionada, LocalDate hoy,
			LocalDate fechaMin, LocalDate fechaMax, Set<LocalDate> diasConAtencion) {
		List<DiaView> dias = new ArrayList<>(7);
		for (int indice = 0; indice < 7; indice++) {
			LocalDate fecha = lunes.plusDays(indice);
			boolean enVentana = !fecha.isBefore(fechaMin) && !fecha.isAfter(fechaMax);
			boolean habilitado = enVentana && diasConAtencion.contains(fecha);
			String etiqueta = null;
			if (fecha.equals(hoy)) {
				etiqueta = "Hoy";
			} else if (enVentana && !habilitado) {
				etiqueta = "Cerrado";
			}
			String descripcion = FormatoVista.capitalizar(fecha.format(DESCRIPCION_DIA_FORMATTER))
					+ (etiqueta == null ? ", no disponible" : ", " + etiqueta.toLowerCase(LOCALE));
			String nombre = FormatoVista.sinPuntos(fecha.getDayOfWeek().getDisplayName(TextStyle.SHORT, LOCALE))
					.toUpperCase(Locale.ROOT);
			dias.add(new DiaView(fecha, nombre, fecha.getDayOfMonth(), descripcion, etiqueta, habilitado,
					fecha.equals(fechaSeleccionada)));
		}
		return dias;
	}

	/**
	 * Bloques del día. Un bloque libre se puede elegir si en él empieza una cita completa (RN-01); al pasar el
	 * cursor muestra la hora en que terminaría esa cita.
	 */
	private static List<SlotView> crearSlots(DisponibilidadDto disponibilidad,
			Optional<InicioDisponibleDto> seleccionado) {
		Map<Long, LocalTime> finPorInicio = disponibilidad.inicios().stream()
				.collect(Collectors.toMap(InicioDisponibleDto::idHorario, InicioDisponibleDto::horaFin,
						(primero, segundo) -> primero));

		return disponibilidad.bloques().stream()
				.map(bloque -> {
					boolean inicioValido = finPorInicio.containsKey(bloque.idHorario());
					boolean dentroDeSeleccion = seleccionado
							.filter(inicio -> !bloque.horaInicio().isBefore(inicio.horaInicio())
									&& bloque.horaInicio().isBefore(inicio.horaFin()))
							.isPresent();
					boolean esSeleccionado = seleccionado
							.filter(inicio -> inicio.idHorario().equals(bloque.idHorario()))
							.isPresent();
					String horaFinCita = HORA.format(finPorInicio.getOrDefault(bloque.idHorario(), bloque.horaFin()));
					return new SlotView(bloque.idHorario(), HORA.format(bloque.horaInicio()), horaFinCita,
							bloque.ocupado(), !bloque.ocupado() && !inicioValido, inicioValido, dentroDeSeleccion,
							esSeleccionado);
				})
				.toList();
	}

	private static String describirAnticipacion(int dias) {
		return switch (dias) {
			case 0 -> "hoy";
			case 1 -> "mañana";
			default -> "dentro de " + dias + " días";
		};
	}

	public record DiaView(LocalDate fecha, String nombre, int numero, String descripcion, String etiqueta,
			boolean habilitado, boolean seleccionado) {
	}

	public record SlotView(Long id, String hora, String horaFin, boolean ocupado, boolean bloqueado,
			boolean seleccionable, boolean dentroSeleccion, boolean seleccionado) {
	}
}
