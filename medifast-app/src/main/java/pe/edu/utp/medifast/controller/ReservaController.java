package pe.edu.utp.medifast.controller;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
import pe.edu.utp.medifast.dto.BloqueHorarioDto;
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

/** Presentación del flujo de selección de fecha y horario (P2). */
@Controller
@RequestMapping("/paciente/reservar")
public class ReservaController {

	private static final Locale LOCALE = Locale.forLanguageTag("es-PE");
	private static final ZoneId ZONA = ZoneId.of("America/Lima");
	private static final DateTimeFormatter MES_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", LOCALE);
	private static final DateTimeFormatter FECHA_FORMATTER =
			DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", LOCALE);
	private static final DateTimeFormatter FECHA_CORTA_FORMATTER =
			DateTimeFormatter.ofPattern("EEE d MMM", LOCALE);
	private static final DateTimeFormatter HORA_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", LOCALE);

	private final ReservaService reservaService;
	private final UsuarioActual usuarioActual;
	private final MedicoDao medicoDao;
	private final HorarioDao horarioDao;

	public ReservaController(ReservaService reservaService, UsuarioActual usuarioActual, MedicoDao medicoDao,
			HorarioDao horarioDao) {
		this.reservaService = reservaService;
		this.usuarioActual = usuarioActual;
		this.medicoDao = medicoDao;
		this.horarioDao = horarioDao;
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
		LocalDate hoy = LocalDate.now(ZONA);
		int diasMin = medico.getConsultorio().getDiasAnticipacionMin();
		int diasMax = medico.getConsultorio().getDiasAnticipacionMax();
		LocalDate fechaMin = hoy.plusDays(diasMin);
		LocalDate fechaMax = hoy.plusDays(diasMax);
		LocalDate fechaSeleccionada = normalizarFecha(fecha, fechaMin, fechaMax);
		DisponibilidadDto disponibilidad =
				reservaService.disponibilidad(idPaciente, idMedico, fechaSeleccionada);

		Long idHorarioSeleccionado = disponibilidad.inicios().stream()
				.filter(inicio -> inicio.idHorario().equals(horarioId))
				.map(InicioDisponibleDto::idHorario)
				.findFirst()
				.orElse(null);
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
				.filter(expiraEn -> expiraEn.isAfter(LocalDateTime.now(ZONA)))
				.isPresent();

		model.addAttribute("medico", medico);
		model.addAttribute("nombrePaciente", paciente.getNombres() + " " + paciente.getApellidos());
		model.addAttribute("nombreSaludo", primerNombre(paciente.getNombres()));
		model.addAttribute("inicialesPaciente", iniciales(paciente.getNombres(), paciente.getApellidos()));
		model.addAttribute("disponibilidad", disponibilidad);
		model.addAttribute("dias", crearDias(lunes, fechaSeleccionada, hoy, fechaMin, fechaMax, diasConAtencion));
		model.addAttribute("hayAnterior", lunes.isAfter(fechaMin));
		model.addAttribute("haySiguiente", !lunes.plusWeeks(1).isAfter(fechaMax));
		model.addAttribute("semanaAnterior", normalizarFecha(fechaSeleccionada.minusWeeks(1), fechaMin, fechaMax));
		model.addAttribute("semanaSiguiente", normalizarFecha(fechaSeleccionada.plusWeeks(1), fechaMin, fechaMax));
		model.addAttribute("mesVisible", capitalizar(fechaSeleccionada.format(MES_FORMATTER)));
		model.addAttribute("fechaCompleta", capitalizar(fechaSeleccionada.format(FECHA_FORMATTER)));
		model.addAttribute("fechaCorta", formatearFechaCorta(fechaSeleccionada));
		model.addAttribute("ventanaDesde", describirAnticipacion(diasMin));
		model.addAttribute("ventanaHasta", describirAnticipacion(diasMax));
		model.addAttribute("nombreMedico", "Dr. " + medico.getNombres() + " " + medico.getApellidos());
		model.addAttribute("inicialesMedico", iniciales(medico.getNombres(), medico.getApellidos()));
		model.addAttribute("especialidad", medico.getEspecialidad());
		model.addAttribute("monto", formatearMonto(disponibilidad.precio()));
		model.addAttribute("minutos", disponibilidad.duracionMin());
		model.addAttribute("cantidadBloques",
				(disponibilidad.duracionMin() + medico.getConsultorio().getMinutosBloque() - 1)
						/ medico.getConsultorio().getMinutosBloque());
		model.addAttribute("tipoNombre", disponibilidad.tipo() == TipoCitaEnum.NUEVO ? "Paciente nuevo" : "Paciente de control");
		model.addAttribute("tipoDescripcion", disponibilidad.tipo() == TipoCitaEnum.NUEVO
				? "Primera consulta · " + disponibilidad.duracionMin() + " minutos"
				: "Consulta de control · " + disponibilidad.duracionMin() + " minutos");
		model.addAttribute("slots", crearSlots(disponibilidad, idHorarioSeleccionado));
		model.addAttribute("horarioId", idHorarioSeleccionado);
		Optional<InicioDisponibleDto> inicioSeleccionado = disponibilidad.inicios().stream()
				.filter(inicio -> inicio.idHorario().equals(idHorarioSeleccionado))
				.findFirst();
		model.addAttribute("resumenHora", inicioSeleccionado
				.map(inicio -> formatearRango(inicio.horaInicio(), inicio.horaFin()))
				.orElse("—"));
		model.addAttribute("resumenMovil", formatearFechaCorta(fechaSeleccionada) + " · " + inicioSeleccionado
				.map(inicio -> HORA_FORMATTER.format(inicio.horaInicio()) + "–"
						+ HORA_FORMATTER.format(inicio.horaFin()))
				.orElse("—"));
		model.addAttribute("errorHorario", "horario".equals(error));
		model.addAttribute("errorFecha", "fecha".equals(error));
		model.addAttribute("citaActiva", citaActiva.orElse(null));
		model.addAttribute("pagoPendiente", pagoPendiente);
		model.addAttribute("segundosRestantes", pagoPendiente
				? Duration.between(LocalDateTime.now(ZONA), citaActiva.get().getExpiraEn()).toSeconds()
				: 0);
		model.addAttribute("estadoActivo", citaActiva.map(cita -> claseEstado(cita, pagoPendiente)).orElse(""));
		model.addAttribute("etiquetaEstadoCita", citaActiva.map(cita -> etiquetaEstado(cita, pagoPendiente))
				.orElse(""));
		model.addAttribute("fechaActiva", citaActiva.map(VistaCita::getFecha)
				.map(ReservaController::formatearFechaCorta).orElse(""));
		model.addAttribute("rangoActivo", citaActiva
				.map(cita -> formatearRango(cita.getHoraInicio(), cita.getHoraFin())).orElse(""));
		model.addAttribute("tipoActivo", citaActiva.map(VistaCita::getTipoCita)
				.map(tipo -> tipo == TipoCitaEnum.NUEVO ? "Nuevo" : "Control").orElse(""));
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
			String descripcion = capitalizar(fecha.format(DateTimeFormatter.ofPattern("EEEE d MMMM", LOCALE)))
					+ (etiqueta == null ? ", no disponible" : ", " + etiqueta.toLowerCase(LOCALE));
			dias.add(new DiaView(fecha, fecha.getDayOfWeek().getDisplayName(TextStyle.SHORT, LOCALE)
					.replace(".", "").toUpperCase(Locale.ROOT), fecha.getDayOfMonth(), descripcion, etiqueta,
					habilitado, fecha.equals(fechaSeleccionada)));
		}
		return dias;
	}

	private static List<SlotView> crearSlots(DisponibilidadDto disponibilidad, Long idSeleccionado) {
		List<InicioDisponibleDto> inicios = disponibilidad.inicios();
		List<Long> iniciosValidos = inicios.stream().map(InicioDisponibleDto::idHorario).toList();
		Optional<InicioDisponibleDto> seleccionado = disponibilidad.inicios().stream()
				.filter(inicio -> inicio.idHorario().equals(idSeleccionado)).findFirst();
		LocalTime horaFinSeleccionada = seleccionado.map(InicioDisponibleDto::horaFin).orElse(null);

		return disponibilidad.bloques().stream()
				.map(bloque -> {
					boolean inicioValido = iniciosValidos.contains(bloque.idHorario());
					boolean dentroDeSeleccion = seleccionado.isPresent()
							&& !bloque.horaInicio().isBefore(seleccionado.get().horaInicio())
							&& bloque.horaInicio().isBefore(horaFinSeleccionada);
					String horaFinCita = inicios.stream()
							.filter(inicio -> inicio.idHorario().equals(bloque.idHorario()))
							.map(InicioDisponibleDto::horaFin)
							.findFirst()
							.map(HORA_FORMATTER::format)
							.orElse(HORA_FORMATTER.format(bloque.horaFin()));
					return new SlotView(bloque.idHorario(), HORA_FORMATTER.format(bloque.horaInicio()),
							horaFinCita, bloque.ocupado(),
							!bloque.ocupado() && !inicioValido, inicioValido, dentroDeSeleccion,
							seleccionado.isPresent() && bloque.idHorario().equals(idSeleccionado));
				})
				.toList();
	}

	/** "Mié 11 nov": el formato es-PE agrega puntos a las abreviaturas. */
	private static String formatearFechaCorta(LocalDate fecha) {
		return capitalizar(fecha.format(FECHA_CORTA_FORMATTER).replace(".", ""));
	}

	/** "10:30 – 11:30 a. m."; el periodo se repite solo si cambia ("11:30 a. m. – 12:30 p. m."). */
	private static String formatearRango(LocalTime inicio, LocalTime fin) {
		return periodo(inicio).equals(periodo(fin))
				? hora12(inicio) + " – " + hora12(fin) + " " + periodo(fin)
				: hora12(inicio) + " " + periodo(inicio) + " – " + hora12(fin) + " " + periodo(fin);
	}

	private static String hora12(LocalTime hora) {
		int hora12 = hora.getHour() % 12 == 0 ? 12 : hora.getHour() % 12;
		return "%d:%02d".formatted(hora12, hora.getMinute());
	}

	private static String periodo(LocalTime hora) {
		return hora.getHour() < 12 ? "a. m." : "p. m.";
	}

	private static String describirAnticipacion(int dias) {
		return switch (dias) {
			case 0 -> "hoy";
			case 1 -> "mañana";
			default -> "dentro de " + dias + " días";
		};
	}

	private static String formatearMonto(BigDecimal monto) {
		NumberFormat formato = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-PE"));
		formato.setMinimumFractionDigits(2);
		formato.setMaximumFractionDigits(2);
		return "S/ " + formato.format(monto);
	}

	private static String capitalizar(String texto) {
		return texto.isEmpty() ? texto : texto.substring(0, 1).toUpperCase(LOCALE) + texto.substring(1);
	}

	private static String iniciales(String nombres, String apellidos) {
		return (primeraInicial(nombres) + primeraInicial(apellidos)).toUpperCase(Locale.ROOT);
	}

	private static String primerNombre(String nombres) {
		return nombres.trim().split("\\s+")[0];
	}

	private static String primeraInicial(String nombres) {
		return nombres.trim().split("\\s+")[0].substring(0, 1);
	}

	/** Etiquetas de README §5.2; una cita PENDIENTE_PAGO vencida que RF16 aún no canceló se muestra "Vencida". */
	private static String etiquetaEstado(VistaCita cita, boolean pagoPendiente) {
		return switch (cita.getEstado()) {
			case PENDIENTE_PAGO -> pagoPendiente ? "Pago pendiente" : "Vencida";
			case PROGRAMADA -> "Confirmada";
			default -> cita.getEstado().name();
		};
	}

	private static String claseEstado(VistaCita cita, boolean pagoPendiente) {
		return switch (cita.getEstado()) {
			case PENDIENTE_PAGO -> pagoPendiente ? "pendiente" : "vencida";
			case PROGRAMADA -> "confirmada";
			default -> "vencida";
		};
	}

	public record DiaView(LocalDate fecha, String nombre, int numero, String descripcion, String etiqueta,
			boolean habilitado, boolean seleccionado) {
	}

	public record SlotView(Long id, String hora, String horaFin, boolean ocupado, boolean bloqueado,
			boolean seleccionable, boolean dentroSeleccion, boolean seleccionado) {
	}
}
