package pe.edu.utp.medifast.controller;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.base.Enums;
import com.google.common.collect.ImmutableMap;

import pe.edu.utp.medifast.dto.AgendaDto;
import pe.edu.utp.medifast.dto.FiltroAgendaDto;
import pe.edu.utp.medifast.dto.FiltroAgendaDto.Vista;
import pe.edu.utp.medifast.model.HorarioSemanal;
import pe.edu.utp.medifast.model.Medico;
import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;
import pe.edu.utp.medifast.security.UsuarioActual;
import pe.edu.utp.medifast.service.GestionCitaService;
import pe.edu.utp.medifast.util.EtiquetasEstado;

/** P7 — Agenda del médico: citas del día o de la semana con filtro por estado (RF04, CU07). */
@Controller
@RequestMapping("/medico/agenda")
public class AgendaController {

	private static final Locale LOCALE = FormatoVista.LOCALE;
	private static final DateTimeFormatter HORA = FormatoVista.HORA;
	private static final DateTimeFormatter FECHA_LARGA = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy",
			LOCALE);
	private static final DateTimeFormatter TITULO_DIA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", LOCALE);

	/** Opciones del filtro "Estado": los 5 estados de la cita con sus etiquetas de README §5.2. */
	private static final Map<String, String> ESTADOS = Arrays.stream(EstadoCitaEnum.values())
			.collect(ImmutableMap.toImmutableMap(EstadoCitaEnum::name, EtiquetasEstado::cita));

	private final GestionCitaService gestionCitaService;
	private final UsuarioActual usuarioActual;
	private final Clock clock;

	public AgendaController(GestionCitaService gestionCitaService, UsuarioActual usuarioActual, Clock clock) {
		this.gestionCitaService = gestionCitaService;
		this.usuarioActual = usuarioActual;
		this.clock = clock;
	}

	/**
	 * Por defecto abre el día de hoy con todos los estados (decisión de diseño de P7). Los parámetros inválidos
	 * no producen error: se vuelve al valor por defecto.
	 */
	@GetMapping
	public String mostrar(@RequestParam(required = false) String vista, @RequestParam(required = false) String fecha,
			@RequestParam(required = false) String estado, Authentication authentication, Model model) {
		LocalDate hoy = LocalDate.now(clock);
		FiltroAgendaDto filtro = new FiltroAgendaDto(
				"semana".equalsIgnoreCase(vista) ? Vista.SEMANA : Vista.DIA,
				leerFecha(fecha, hoy),
				Enums.getIfPresent(EstadoCitaEnum.class, StringUtils.defaultString(estado)).orNull());
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
				: capitalizar(FECHA_LARGA.format(filtro.fecha())));
		model.addAttribute("etiquetaFecha", semana
				? etiquetaSemana(filtro.desde(), finSemana)
				: FormatoVista.fechaCorta(filtro.fecha()));
		model.addAttribute("etiquetaTotal", semana ? "Citas de la semana" : "Citas del día");
		model.addAttribute("notaTotal", semana
				? diasDeAtencion(agenda.horario())
				: rangoDeAtencion(agenda.horario(), filtro.fecha().getDayOfWeek()));
		model.addAttribute("agenda", agenda);
		model.addAttribute("dias", agruparPorDia(agenda.citas()));
		model.addAttribute("cantidad", agenda.citas().size());
		model.addAttribute("estados", ESTADOS);
		return "medico/agenda";
	}

	private static LocalDate leerFecha(String fecha, LocalDate hoy) {
		if (StringUtils.isBlank(fecha)) {
			return hoy;
		}
		try {
			return LocalDate.parse(fecha.strip());
		} catch (DateTimeParseException e) {
			return hoy;
		}
	}

	private static List<DiaAgenda> agruparPorDia(List<VistaCita> citas) {
		Map<LocalDate, List<FilaAgenda>> porDia = new LinkedHashMap<>();
		for (VistaCita cita : citas) {
			porDia.computeIfAbsent(cita.getFecha(), dia -> new ArrayList<>()).add(fila(cita));
		}
		return porDia.entrySet().stream()
				.map(dia -> new DiaAgenda(capitalizar(TITULO_DIA.format(dia.getKey())), dia.getValue()))
				.toList();
	}

	private static FilaAgenda fila(VistaCita cita) {
		return new FilaAgenda(
				cita.getIdCita(),
				HORA.format(cita.getHoraInicio()),
				Duration.between(cita.getHoraInicio(), cita.getHoraFin()).toMinutes(),
				iniciales(cita.getPaciente()),
				cita.getPaciente(),
				FormatoVista.documento(cita.getTipoDocumento(), cita.getNumeroDocumento()),
				EtiquetasEstado.tipo(cita.getTipoCita()),
				EtiquetasEstado.cita(cita.getEstado()),
				EtiquetasEstado.claseCita(cita.getEstado()),
				cita.getEstadoPago() == null ? null : EtiquetasEstado.pago(cita.getEstadoPago()),
				EtiquetasEstado.clasePago(cita.getEstadoPago()));
	}

	/**
	 * Inicial del primer nombre y del apellido paterno. {@code v_cita} trae "nombres apellidos" en un solo campo;
	 * con 3 palabras o más se toma la penúltima como apellido paterno ("Andrea Sofía Salazar Pérez" → "AS").
	 */
	static String iniciales(String nombreCompleto) {
		String[] palabras = StringUtils.split(StringUtils.defaultString(nombreCompleto));
		if (palabras.length == 0) {
			return "?";
		}
		if (palabras.length == 1) {
			return palabras[0].substring(0, 1).toUpperCase(LOCALE);
		}
		String apellido = palabras.length >= 3 ? palabras[palabras.length - 2] : palabras[1];
		return (palabras[0].substring(0, 1) + apellido.substring(0, 1)).toUpperCase(LOCALE);
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
			return capitalizar(nombreDia(dias.get(0))) + " a " + nombreDia(dias.get(dias.size() - 1));
		}
		List<String> nombres = dias.stream().map(AgendaController::nombreDia).toList();
		return capitalizar(String.join(", ", nombres.subList(0, nombres.size() - 1)) + " y "
				+ nombres.get(nombres.size() - 1));
	}

	private static String nombreDia(DayOfWeek dia) {
		return dia.getDisplayName(TextStyle.FULL, LOCALE);
	}

	/** "Semana del 9 al 14 de noviembre de 2026"; repite el mes o el año solo si cambian. */
	private static String subtituloSemana(LocalDate desde, LocalDate hasta) {
		String fin = hasta.getDayOfMonth() + " de " + mes(hasta, TextStyle.FULL) + " de " + hasta.getYear();
		String inicio = desde.getDayOfMonth()
				+ (desde.getMonth() == hasta.getMonth() ? "" : " de " + mes(desde, TextStyle.FULL))
				+ (desde.getYear() == hasta.getYear() ? "" : " de " + desde.getYear());
		return "Semana del " + inicio + " al " + fin;
	}

	/** "9 – 14 nov 2026"; repite el mes o el año solo si cambian. */
	private static String etiquetaSemana(LocalDate desde, LocalDate hasta) {
		String fin = hasta.getDayOfMonth() + " " + mes(hasta, TextStyle.SHORT) + " " + hasta.getYear();
		String inicio = desde.getDayOfMonth()
				+ (desde.getMonth() == hasta.getMonth() ? "" : " " + mes(desde, TextStyle.SHORT))
				+ (desde.getYear() == hasta.getYear() ? "" : " " + desde.getYear());
		return inicio + " – " + fin;
	}

	private static String mes(LocalDate fecha, TextStyle estilo) {
		return FormatoVista.mes(fecha, estilo);
	}

	private static String capitalizar(String texto) {
		return FormatoVista.capitalizar(texto);
	}

	/** Citas de un día; en la vista semana, cada día lleva su cabecera con el contador. */
	public record DiaAgenda(String titulo, List<FilaAgenda> citas) {

		public String contador() {
			return citas.size() == 1 ? "1 cita" : citas.size() + " citas";
		}
	}

	/**
	 * Fila de la agenda. {@code claseCita}/{@code clasePago} eligen el color del badge; {@code etiquetaPago} es
	 * {@code null} mientras la cita no tiene pago (se muestra "—").
	 */
	public record FilaAgenda(Long id, String hora, long minutos, String iniciales, String paciente, String documento,
			String tipo, String etiquetaCita, String claseCita, String etiquetaPago, String clasePago) {
	}
}
