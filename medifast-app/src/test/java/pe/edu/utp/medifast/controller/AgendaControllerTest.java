package pe.edu.utp.medifast.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pe.edu.utp.medifast.config.SecurityConfig;
import pe.edu.utp.medifast.dao.UsuarioDao;
import pe.edu.utp.medifast.dto.AgendaDto;
import pe.edu.utp.medifast.dto.FiltroAgendaDto;
import pe.edu.utp.medifast.dto.FiltroAgendaDto.Vista;
import pe.edu.utp.medifast.model.HorarioSemanal;
import pe.edu.utp.medifast.model.Medico;
import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;
import pe.edu.utp.medifast.model.enums.EstadoPagoEnum;
import pe.edu.utp.medifast.model.enums.TipoCitaEnum;
import pe.edu.utp.medifast.model.enums.TipoDocumentoEnum;
import pe.edu.utp.medifast.security.EventosAutenticacion;
import pe.edu.utp.medifast.security.IntentosLoginService;
import pe.edu.utp.medifast.security.RedireccionPorRol;
import pe.edu.utp.medifast.security.UsuarioActual;
import pe.edu.utp.medifast.security.UsuarioDetailsService;
import pe.edu.utp.medifast.service.GestionCitaService;
import pe.edu.utp.medifast.util.Auditoria;

@WebMvcTest(AgendaController.class)
@Import({ SecurityConfig.class, RedireccionPorRol.class, UsuarioDetailsService.class,
		IntentosLoginService.class, EventosAutenticacion.class, AgendaControllerTest.Reloj.class })
class AgendaControllerTest {

	private static final long ID_MEDICO = 1L;
	private static final LocalDate MIERCOLES = LocalDate.of(2026, 11, 11);

	/** Hoy es el miércoles 11 de noviembre de 2026, como en el diseño aprobado. */
	@TestConfiguration
	static class Reloj {
		@Bean
		Clock clock() {
			return Clock.fixed(Instant.parse("2026-11-11T16:45:00Z"), ZoneId.of("America/Lima"));
		}
	}

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GestionCitaService gestionCitaService;
	@MockitoBean
	private UsuarioActual usuarioActual;
	@MockitoBean
	private UsuarioDao usuarioDao;
	@MockitoBean
	private Auditoria auditoria;

	@BeforeEach
	void configurar() {
		Medico medico = mock(Medico.class);
		when(medico.getId()).thenReturn(ID_MEDICO);
		when(medico.getNombres()).thenReturn("Moisés");
		when(medico.getApellidos()).thenReturn("Barrantes Cabrera");
		when(usuarioActual.medico(any())).thenReturn(medico);
	}

	@Test
	void porDefectoMuestraHoyConTodosLosEstados() throws Exception {
		AgendaDto agenda = agendaDelMiercoles();
		when(gestionCitaService.agenda(eq(ID_MEDICO), any())).thenReturn(agenda);

		mockMvc.perform(get("/medico/agenda").with(user("10000001").roles("MEDICO")))
				.andExpect(status().isOk())
				.andExpect(view().name("medico/agenda"))
				.andExpect(model().attribute("vista", "dia"))
				.andExpect(content().string(allOf(
						containsString("Dr. Moisés Barrantes"),
						containsString("Miércoles, 11 de noviembre de 2026"),
						containsString("Mié 11 nov 2026"),
						containsString("Citas del día"),
						containsString("De 09:00 a 13:00"),
						containsString("Carlos Mendoza Ríos"),
						containsString("DNI ••••9452"),
						containsString(">CM<"),
						containsString(">AS<"),
						containsString("60 min"),
						containsString("Atendida"),
						containsString("Verificado"),
						containsString("Confirmada"),
						containsString("Por validar"),
						containsString("Pago pendiente"),
						containsString("/medico/citas/1?vista=dia&amp;fecha=2026-11-11"),
						not(containsString("70009452")),
						not(containsString("Quitar filtro")))));

		FiltroAgendaDto filtro = filtroUsado();
		assertThat(filtro.vista()).isEqualTo(Vista.DIA);
		assertThat(filtro.fecha()).isEqualTo(MIERCOLES);
		assertThat(filtro.estado()).isNull();
	}

	@Test
	void vistaSemanaAgrupaPorDiaYRotulaDeLunesASabado() throws Exception {
		List<VistaCita> citas = new ArrayList<>();
		citas.add(cita(10L, LocalDate.of(2026, 11, 9), LocalTime.of(9, 30), "Roberto Castro Silva", "70003214",
				TipoCitaEnum.CONTROL, EstadoCitaEnum.ATENDIDA, EstadoPagoEnum.VALIDADO));
		citas.add(cita(11L, LocalDate.of(2026, 11, 9), LocalTime.of(11, 0), "Valeria Gómez Morales", "70008765",
				TipoCitaEnum.NUEVO, EstadoCitaEnum.ATENDIDA, EstadoPagoEnum.VALIDADO));
		citas.addAll(agendaDelMiercoles().citas());
		AgendaDto agenda = new AgendaDto(citas, 8, 1, 2, 3, horarioLunesASabado());
		when(gestionCitaService.agenda(eq(ID_MEDICO), any())).thenReturn(agenda);

		mockMvc.perform(get("/medico/agenda").param("vista", "semana").param("fecha", "2026-11-11")
				.with(user("10000001").roles("MEDICO")))
				.andExpect(status().isOk())
				.andExpect(model().attribute("vista", "semana"))
				.andExpect(model().attribute("anterior", LocalDate.of(2026, 11, 4)))
				.andExpect(content().string(allOf(
						containsString("Semana del 9 al 14 de noviembre de 2026"),
						containsString("9 – 14 nov 2026"),
						containsString("Citas de la semana"),
						containsString("Lunes a sábado"),
						containsString("Lunes 9 de noviembre"),
						containsString("2 citas"),
						containsString("Miércoles 11 de noviembre"),
						containsString("6 citas"))));

		assertThat(filtroUsado().vista()).isEqualTo(Vista.SEMANA);
	}

	@Test
	void filtroPorEstadoMuestraResumenYQuitarFiltro() throws Exception {
		List<VistaCita> confirmadas = agendaDelMiercoles().citas().stream()
				.filter(cita -> cita.getEstado() == EstadoCitaEnum.PROGRAMADA)
				.toList();
		AgendaDto agenda = new AgendaDto(confirmadas, 6, 1, 2, 1, horarioLunesASabado());
		when(gestionCitaService.agenda(eq(ID_MEDICO), any())).thenReturn(agenda);

		mockMvc.perform(get("/medico/agenda").param("estado", "PROGRAMADA")
				.with(user("10000001").roles("MEDICO")))
				.andExpect(status().isOk())
				.andExpect(content().string(allOf(
						containsString("2 citas · Confirmada"),
						containsString("Quitar filtro"),
						containsString("<option value=\"PROGRAMADA\" selected=\"selected\">Confirmada</option>"),
						not(containsString("Carlos Mendoza Ríos")))));

		assertThat(filtroUsado().estado()).isEqualTo(EstadoCitaEnum.PROGRAMADA);
	}

	@Test
	void diaSinCitasMuestraElEstadoVacio() throws Exception {
		AgendaDto agenda = new AgendaDto(List.of(), 0, 0, 0, 0, horarioLunesASabado());
		when(gestionCitaService.agenda(eq(ID_MEDICO), any())).thenReturn(agenda);

		mockMvc.perform(get("/medico/agenda").param("fecha", "2026-11-12")
				.with(user("10000001").roles("MEDICO")))
				.andExpect(status().isOk())
				.andExpect(content().string(allOf(
						containsString("Jueves, 12 de noviembre de 2026"),
						containsString("No hay citas para este día"),
						containsString("Ver la semana"))));
	}

	@Test
	void parametrosInvalidosVuelvenALosValoresPorDefecto() throws Exception {
		when(gestionCitaService.agenda(eq(ID_MEDICO), any()))
				.thenReturn(new AgendaDto(List.of(), 0, 0, 0, 0, List.of()));

		mockMvc.perform(get("/medico/agenda").param("vista", "mes").param("fecha", "no-es-fecha")
				.param("estado", "INEXISTENTE").with(user("10000001").roles("MEDICO")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Sin horario de atención")));

		FiltroAgendaDto filtro = filtroUsado();
		assertThat(filtro.vista()).isEqualTo(Vista.DIA);
		assertThat(filtro.fecha()).isEqualTo(MIERCOLES);
		assertThat(filtro.estado()).isNull();
	}

	@Test
	void pacienteNoAccedeALaAgenda() throws Exception {
		mockMvc.perform(get("/medico/agenda").with(user("70000001").roles("PACIENTE")))
				.andExpect(status().isForbidden());
	}

	@Test
	void sinSesionRedirigeAlLogin() throws Exception {
		mockMvc.perform(get("/medico/agenda"))
				.andExpect(status().is3xxRedirection());
	}

	@Test
	void inicialesDelNombreYDelApellidoPaterno() {
		assertThat(AgendaController.iniciales("Andrea Sofía Salazar Pérez")).isEqualTo("AS");
		assertThat(AgendaController.iniciales("Carlos Mendoza Ríos")).isEqualTo("CM");
		assertThat(AgendaController.iniciales("Ana Díaz")).isEqualTo("AD");
		assertThat(AgendaController.iniciales("Ana")).isEqualTo("A");
		assertThat(AgendaController.iniciales(null)).isEqualTo("?");
	}

	private FiltroAgendaDto filtroUsado() {
		ArgumentCaptor<FiltroAgendaDto> filtro = ArgumentCaptor.forClass(FiltroAgendaDto.class);
		verify(gestionCitaService).agenda(eq(ID_MEDICO), filtro.capture());
		return filtro.getValue();
	}

	/** Datos de ejemplo del diseño aprobado (P7 v2). */
	private static AgendaDto agendaDelMiercoles() {
		List<VistaCita> citas = List.of(
				cita(1L, MIERCOLES, LocalTime.of(9, 0), "Carlos Mendoza Ríos", "70009452", TipoCitaEnum.NUEVO,
						EstadoCitaEnum.ATENDIDA, EstadoPagoEnum.VALIDADO),
				cita(2L, MIERCOLES, LocalTime.of(10, 0), "Lucía Torres Paredes", "70007405", TipoCitaEnum.CONTROL,
						EstadoCitaEnum.NO_ASISTIO, EstadoPagoEnum.VALIDADO),
				cita(3L, MIERCOLES, LocalTime.of(10, 30), "Andrea Sofía Salazar Pérez", "70005678",
						TipoCitaEnum.NUEVO, EstadoCitaEnum.PROGRAMADA, EstadoPagoEnum.POR_VALIDAR),
				cita(4L, MIERCOLES, LocalTime.of(11, 30), "José Ramírez Castillo", "70007362", TipoCitaEnum.CONTROL,
						EstadoCitaEnum.PROGRAMADA, EstadoPagoEnum.VALIDADO),
				cita(5L, MIERCOLES, LocalTime.of(12, 0), "María Rojas Vega", "70008193", TipoCitaEnum.CONTROL,
						EstadoCitaEnum.PENDIENTE_PAGO, null),
				cita(6L, MIERCOLES, LocalTime.of(12, 30), "Pedro Huamán Díaz", "70003018", TipoCitaEnum.CONTROL,
						EstadoCitaEnum.CANCELADA, EstadoPagoEnum.RECHAZADO));
		return new AgendaDto(citas, 6, 1, 2, 1, horarioLunesASabado());
	}

	private static List<HorarioSemanal> horarioLunesASabado() {
		List<HorarioSemanal> horario = new ArrayList<>();
		for (DayOfWeek dia : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
				DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)) {
			HorarioSemanal bloque = mock(HorarioSemanal.class);
			when(bloque.getDiaSemana()).thenReturn(dia);
			when(bloque.getHoraInicio()).thenReturn(LocalTime.of(9, 0));
			when(bloque.getHoraFin()).thenReturn(LocalTime.of(13, 0));
			horario.add(bloque);
		}
		return horario;
	}

	private static VistaCita cita(long id, LocalDate fecha, LocalTime inicio, String paciente, String documento,
			TipoCitaEnum tipo, EstadoCitaEnum estado, EstadoPagoEnum estadoPago) {
		VistaCita cita = mock(VistaCita.class);
		when(cita.getIdCita()).thenReturn(id);
		when(cita.getFecha()).thenReturn(fecha);
		when(cita.getHoraInicio()).thenReturn(inicio);
		when(cita.getHoraFin()).thenReturn(inicio.plusMinutes(tipo == TipoCitaEnum.NUEVO ? 60 : 30));
		when(cita.getPaciente()).thenReturn(paciente);
		when(cita.getTipoDocumento()).thenReturn(TipoDocumentoEnum.DNI);
		when(cita.getNumeroDocumento()).thenReturn(documento);
		when(cita.getTipoCita()).thenReturn(tipo);
		when(cita.getEstado()).thenReturn(estado);
		when(cita.getEstadoPago()).thenReturn(estadoPago);
		return cita;
	}
}
