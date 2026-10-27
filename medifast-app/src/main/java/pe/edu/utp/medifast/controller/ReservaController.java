package pe.edu.utp.medifast.controller;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pe.edu.utp.medifast.dao.MedicoDao;
import pe.edu.utp.medifast.dto.BloqueHorarioDto;
import pe.edu.utp.medifast.dto.DisponibilidadDto;
import pe.edu.utp.medifast.dto.InicioDisponibleDto;
import pe.edu.utp.medifast.exception.CitaActivaException;
import pe.edu.utp.medifast.exception.FueraDeRangoException;
import pe.edu.utp.medifast.exception.HorarioNoDisponibleException;
import pe.edu.utp.medifast.model.Cita;
import pe.edu.utp.medifast.model.Medico;
import pe.edu.utp.medifast.model.Paciente;
import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.model.enums.TipoCitaEnum;
import pe.edu.utp.medifast.security.UsuarioActual;
import pe.edu.utp.medifast.service.ReservaService;

/** Presentación del flujo de selección de fecha y horario (P2). */
@Controller
@RequestMapping("/paciente/reservar")
public class ReservaController {

	private static final Locale LOCALE = Locale.forLanguageTag("es-PE");
	private static final List<DayOfWeek> DIAS_ATENCION = List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
			DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);
	private static final DateTimeFormatter MES_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", LOCALE);
	private static final DateTimeFormatter FECHA_FORMATTER =
			DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", LOCALE);
	private static final DateTimeFormatter FECHA_CORTA_FORMATTER =
			DateTimeFormatter.ofPattern("EEE d MMM", LOCALE);
	private static final DateTimeFormatter HORA_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", LOCALE);

	private final ReservaService reservaService;
	private final UsuarioActual usuarioActual;
	private final MedicoDao medicoDao;

	public ReservaController(ReservaService reservaService, UsuarioActual usuarioActual, MedicoDao medicoDao) {
		this.reservaService = reservaService;
		this.usuarioActual = usuarioActual;
		this.medicoDao = medicoDao;
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
		LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));
		LocalDate fechaMin = hoy.plusDays(medico.getConsultorio().getDiasAnticipacionMin());
		LocalDate fechaMax = hoy.plusDays(medico.getConsultorio().getDiasAnticipacionMax());
		LocalDate fechaSeleccionada = normalizarFecha(fecha, fechaMin, fechaMax);
		DisponibilidadDto disponibilidad =
				reservaService.disponibilidad(idPaciente, idMedico, fechaSeleccionada);

		Long idHorarioSeleccionado = disponibilidad.inicios().stream()
				.filter(inicio -> inicio.idHorario().equals(horarioId))
				.map(InicioDisponibleDto::idHorario)
				.findFirst()
				.orElse(null);

		model.addAttribute("medico", medico);
		model.addAttribute("nombrePaciente", paciente.getNombres() + " " + paciente.getApellidos());
		model.addAttribute("nombreSaludo", primerNombre(paciente.getNombres()));
		model.addAttribute("inicialesPaciente", iniciales(paciente.getNombres(), paciente.getApellidos()));
		model.addAttribute("disponibilidad", disponibilidad);
		model.addAttribute("dias", crearDias(fechaSeleccionada, fechaMin, fechaMax));
		model.addAttribute("semanaAnterior", normalizarFecha(fechaSeleccionada.minusWeeks(1), fechaMin, fechaMax));
		model.addAttribute("semanaSiguiente", normalizarFecha(fechaSeleccionada.plusWeeks(1), fechaMin, fechaMax));
		model.addAttribute("mesVisible", capitalizar(fechaSeleccionada.format(MES_FORMATTER)));
		model.addAttribute("fechaCompleta", capitalizar(fechaSeleccionada.format(FECHA_FORMATTER)));
		model.addAttribute("fechaCorta", capitalizar(fechaSeleccionada.format(FECHA_CORTA_FORMATTER)));
		model.addAttribute("fechaMinTexto", capitalizar(fechaMin.format(FECHA_CORTA_FORMATTER)));
		model.addAttribute("fechaMaxTexto", capitalizar(fechaMax.format(FECHA_CORTA_FORMATTER)));
		model.addAttribute("nombreMedico", "Dr. " + medico.getNombres() + " " + medico.getApellidos());
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
		model.addAttribute("resumenHora", buscarRango(disponibilidad, idHorarioSeleccionado));
		model.addAttribute("errorHorario", "horario".equals(error));
		model.addAttribute("errorFecha", "fecha".equals(error));
		model.addAttribute("citaActiva", disponibilidad.citaActiva().orElse(null));
		model.addAttribute("etiquetaEstadoCita", disponibilidad.citaActiva()
				.map(VistaCita::getEstado).map(Enum::name).map(ReservaController::etiquetaEstado)
				.orElse(""));
		model.addAttribute("fechaActiva", disponibilidad.citaActiva().map(VistaCita::getFecha)
				.map(ReservaController::formatearFechaCorta).orElse(""));
		model.addAttribute("horaActiva", disponibilidad.citaActiva().map(VistaCita::getHoraInicio)
				.map(ReservaController::formatearHora).orElse(""));
		model.addAttribute("horaFinActiva", disponibilidad.citaActiva().map(VistaCita::getHoraFin)
				.map(ReservaController::formatearHora).orElse(""));
		model.addAttribute("tipoActivo", disponibilidad.citaActiva().map(VistaCita::getTipoCita)
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

	private static List<DiaView> crearDias(LocalDate fechaSeleccionada, LocalDate fechaMin, LocalDate fechaMax) {
		LocalDate lunes = fechaSeleccionada.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		List<DiaView> dias = new ArrayList<>(7);
		for (int indice = 0; indice < 7; indice++) {
			LocalDate fecha = lunes.plusDays(indice);
			boolean habilitado = !fecha.isBefore(fechaMin) && !fecha.isAfter(fechaMax)
					&& DIAS_ATENCION.contains(fecha.getDayOfWeek());
			dias.add(new DiaView(fecha, fecha.getDayOfWeek().getDisplayName(TextStyle.SHORT, LOCALE)
					.replace(".", "").toUpperCase(Locale.ROOT), fecha.getDayOfMonth(),
					capitalizar(fecha.format(DateTimeFormatter.ofPattern("EEEE d MMMM", LOCALE))), habilitado,
					fecha.equals(fechaSeleccionada)));
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
							!bloque.disponible() && !bloque.ocupado(), inicioValido, dentroDeSeleccion,
							seleccionado.isPresent() && bloque.idHorario().equals(idSeleccionado));
				})
				.toList();
	}

	private static String buscarRango(DisponibilidadDto disponibilidad, Long idHorario) {
		return disponibilidad.inicios().stream()
				.filter(inicio -> inicio.idHorario().equals(idHorario))
				.map(inicio -> formatearHora(inicio.horaInicio()) + "–" + formatearHora(inicio.horaFin()))
				.findFirst()
				.orElse("—");
	}

	private static String formatearFechaCorta(LocalDate fecha) {
		return capitalizar(fecha.format(FECHA_CORTA_FORMATTER));
	}

	private static String formatearHora(LocalTime hora) {
		int hora12 = hora.getHour() % 12 == 0 ? 12 : hora.getHour() % 12;
		String periodo = hora.getHour() < 12 ? "a. m." : "p. m.";
		return "%d:%02d %s".formatted(hora12, hora.getMinute(), periodo);
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

	private static String etiquetaEstado(String estado) {
		return switch (estado) {
			case "PENDIENTE_PAGO" -> "Pago pendiente";
			case "PROGRAMADA" -> "Confirmada";
			default -> estado;
		};
	}

	public record DiaView(LocalDate fecha, String nombre, int numero, String descripcion, boolean habilitado,
			boolean seleccionado) {
	}

	public record SlotView(Long id, String hora, String horaFin, boolean ocupado, boolean bloqueado,
			boolean seleccionable, boolean dentroSeleccion, boolean seleccionado) {
	}
}
