package pe.edu.utp.medifast.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pe.edu.utp.medifast.config.SecurityConfig;
import pe.edu.utp.medifast.dao.UsuarioDao;
import pe.edu.utp.medifast.dto.AgendaDto;
import pe.edu.utp.medifast.dto.ConstanciaDto;
import pe.edu.utp.medifast.dto.DetalleCitaDto;
import pe.edu.utp.medifast.dto.FiltroAgendaDto;
import pe.edu.utp.medifast.dto.FiltroAgendaDto.Vista;
import pe.edu.utp.medifast.model.HorarioSemanal;
import pe.edu.utp.medifast.exception.AccesoDenegadoException;
import pe.edu.utp.medifast.exception.PagoNoValidadoException;
import pe.edu.utp.medifast.exception.RecursoNoEncontradoException;
import pe.edu.utp.medifast.model.Medico;
import pe.edu.utp.medifast.model.Pago;
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
import pe.edu.utp.medifast.service.ConstanciaService;
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
	private ConstanciaService constanciaService;
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
		when(usuarioActual.idMedico(any())).thenReturn(ID_MEDICO);
		when(constanciaService.paraMedico(any(), any()))
				.thenThrow(new RecursoNoEncontradoException("No se encontró el archivo de la constancia."));
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
	void vistaSemanaPaginaLasCitasYConservaElTotalDeCadaDia() throws Exception {
		List<VistaCita> citas = new ArrayList<>();
		for (int i = 0; i < 12; i++) {
			LocalDate dia = i < 8 ? LocalDate.of(2026, 11, 9) : MIERCOLES;
			citas.add(cita(20L + i, dia, LocalTime.of(8, 0).plusMinutes(30L * (i % 8)), "Paciente " + (char) ('A' + i),
					"7000" + (1000 + i), TipoCitaEnum.CONTROL, EstadoCitaEnum.PROGRAMADA, EstadoPagoEnum.POR_VALIDAR));
		}
		AgendaDto agenda = new AgendaDto(citas, 12, 0, 12, 0, horarioLunesASabado());
		when(gestionCitaService.agenda(eq(ID_MEDICO), any())).thenReturn(agenda);

		mockMvc.perform(get("/medico/agenda").param("vista", "semana").param("fecha", "2026-11-11")
				.param("pagina", "2").with(user("10000001").roles("MEDICO")))
				.andExpect(status().isOk())
				.andExpect(model().attribute("rango", "Mostrando 11–12 de 12 citas"))
				.andExpect(content().string(allOf(
						containsString("Paciente K"),
						containsString("Paciente L"),
						not(containsString("Paciente J")),
						not(containsString("Lunes 9 de noviembre")),
						containsString("Miércoles 11 de noviembre"),
						containsString("4 citas"),
						containsString("/medico/agenda?vista=semana&amp;fecha=2026-11-11&amp;estado=&amp;pagina=1"))));
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
	void detalleMuestraLaCitaConElPagoPorValidar() throws Exception {
		DetalleCitaDto detalle = detalleDeAndrea(EstadoPagoEnum.POR_VALIDAR);
		when(gestionCitaService.detalle(124L, ID_MEDICO)).thenReturn(detalle);
		doReturn(new ConstanciaDto(new ByteArrayResource(new byte[1_258_291]), "image/jpeg", "constancia-000124.jpg"))
				.when(constanciaService).paraMedico(124L, ID_MEDICO);

		mockMvc.perform(get("/medico/citas/124").param("vista", "dia").param("fecha", "2026-11-11")
				.with(user("10000001").roles("MEDICO")))
				.andExpect(status().isOk())
				.andExpect(view().name("medico/detalle-cita"))
				.andExpect(content().string(allOf(
						containsString("Cita #000124"),
						containsString("Miércoles, 11 de noviembre de 2026 · 10:30–11:30 a. m."),
						containsString("Andrea Sofía Salazar Pérez"),
						containsString("DNI 41235678"),
						containsString(">AS<"),
						containsString("Nuevo · 60 min"),
						containsString("S/ 150.00"),
						containsString("933731536"),
						containsString("Confirmada"),
						containsString("Por validar"),
						containsString("Validar pago"),
						containsString("Rechazar pago"),
						containsString("Disponible cuando el pago esté verificado."),
						containsString("No asistió"),
						containsString("Cancelar cita"),
						containsString("constancia-000124.jpg"),
						containsString("1.2 MB"),
						containsString("25789222"),
						containsString("Lun 9 nov 2026, 10:12 a. m."),
						containsString("/medico/agenda?vista=dia&amp;fecha=2026-11-11"),
						not(containsString("Estado de la cita")))));
	}

	@Test
	void detalleSinFiltrosVuelveAlDiaDeLaCita() throws Exception {
		DetalleCitaDto detalle = detalleDeAndrea(EstadoPagoEnum.VALIDADO);
		when(gestionCitaService.detalle(124L, ID_MEDICO)).thenReturn(detalle);

		mockMvc.perform(get("/medico/citas/124").with(user("10000001").roles("MEDICO")))
				.andExpect(status().isOk())
				.andExpect(content().string(allOf(
						containsString("/medico/agenda?vista=dia&amp;fecha=2026-11-11"),
						containsString("Verificado"),
						containsString("Marcar atendida"),
						not(containsString("Validar pago")),
						not(containsString("Disponible cuando el pago esté verificado.")),
						containsString("No se encontró el archivo de la constancia."))));
	}

	@Test
	void validarPagoVuelveAlDetalleConLaAlertaYLosFiltros() throws Exception {
		mockMvc.perform(post("/medico/citas/124/validar").param("vista", "semana").param("fecha", "2026-11-11")
				.param("estado", "PROGRAMADA").with(csrf()).with(user("10000001").roles("MEDICO")))
				.andExpect(redirectedUrl("/medico/citas/124?vista=semana&fecha=2026-11-11&estado=PROGRAMADA"))
				.andExpect(flash().attribute("exito", "Pago validado."));

		verify(gestionCitaService).validarPago(124L, ID_MEDICO);
	}

	@Test
	void marcarAtendidaSinPagoValidadoMuestraLaRegla() throws Exception {
		doThrow(new PagoNoValidadoException()).when(gestionCitaService).marcarAtendida(124L, ID_MEDICO);

		mockMvc.perform(post("/medico/citas/124/atendida").with(csrf()).with(user("10000001").roles("MEDICO")))
				.andExpect(redirectedUrl("/medico/citas/124?vista=dia"))
				.andExpect(flash().attribute("error", "Primero valida el pago."));
	}

	@Test
	void rechazarSinMotivoReabreElModalConElError() throws Exception {
		DetalleCitaDto detalle = detalleDeAndrea(EstadoPagoEnum.POR_VALIDAR);
		when(gestionCitaService.detalle(124L, ID_MEDICO)).thenReturn(detalle);

		mockMvc.perform(post("/medico/citas/124/rechazar").param("observacion", "   ").with(csrf())
				.with(user("10000001").roles("MEDICO")))
				.andExpect(status().isOk())
				.andExpect(view().name("medico/detalle-cita"))
				.andExpect(model().attribute("modalAbierto", "rechazar"))
				.andExpect(model().attribute("errorMotivo", "Ingresa el motivo."))
				.andExpect(content().string(containsString("data-abierto=\"true\"")));

		verify(gestionCitaService, never()).rechazarPago(any(), any(), any());
	}

	@Test
	void cancelarConMotivoMuyLargoNoLlamaAlServicio() throws Exception {
		DetalleCitaDto detalle = detalleDeAndrea(EstadoPagoEnum.POR_VALIDAR);
		when(gestionCitaService.detalle(124L, ID_MEDICO)).thenReturn(detalle);

		mockMvc.perform(post("/medico/citas/124/cancelar").param("observacion", "x".repeat(501)).with(csrf())
				.with(user("10000001").roles("MEDICO")))
				.andExpect(status().isOk())
				.andExpect(model().attribute("modalAbierto", "cancelar"))
				.andExpect(model().attribute("errorMotivo", "El motivo no puede superar los 500 caracteres."));

		verify(gestionCitaService, never()).cancelar(any(), any(), any());
	}

	@Test
	void cancelarConMotivoLiberaElHorario() throws Exception {
		mockMvc.perform(post("/medico/citas/124/cancelar").param("observacion", "El paciente viajó.")
				.param("fecha", "2026-11-11").with(csrf()).with(user("10000001").roles("MEDICO")))
				.andExpect(redirectedUrl("/medico/citas/124?vista=dia&fecha=2026-11-11"))
				.andExpect(flash().attribute("exito", "Cita cancelada. El horario quedó libre."));

		verify(gestionCitaService).cancelar(124L, ID_MEDICO, "El paciente viajó.");
	}

	@Test
	void constanciaSeAbreEnLineaConSuTipo() throws Exception {
		doReturn(new ConstanciaDto(new ByteArrayResource(new byte[] { 1, 2, 3 }), "image/png", "constancia-000124.png"))
				.when(constanciaService).paraMedico(124L, ID_MEDICO);

		mockMvc.perform(get("/medico/citas/124/constancia").with(user("10000001").roles("MEDICO")))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "image/png"))
				.andExpect(header().string("Content-Disposition", containsString("inline")));
	}

	@Test
	void citaDeOtroMedicoResponde403() throws Exception {
		when(gestionCitaService.detalle(200L, ID_MEDICO))
				.thenThrow(new AccesoDenegadoException("La cita no pertenece al médico autenticado."));

		mockMvc.perform(get("/medico/citas/200").with(user("10000001").roles("MEDICO")))
				.andExpect(status().isForbidden());
	}

	@Test
	void pacienteNoAccedeAlDetalle() throws Exception {
		mockMvc.perform(get("/medico/citas/124").with(user("70000001").roles("PACIENTE")))
				.andExpect(status().isForbidden());
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

	/** Cita #000124 de Andrea del diseño aprobado de P8. */
	private static DetalleCitaDto detalleDeAndrea(EstadoPagoEnum estadoPago) {
		VistaCita cita = cita(124L, MIERCOLES, LocalTime.of(10, 30), "Andrea Sofía Salazar Pérez", "41235678",
				TipoCitaEnum.NUEVO, EstadoCitaEnum.PROGRAMADA, estadoPago);
		Pago pago = mock(Pago.class);
		when(pago.getEstado()).thenReturn(estadoPago);
		when(pago.getNroOperacion()).thenReturn("25789222");
		when(pago.getFechaPago()).thenReturn(LocalDateTime.of(2026, 11, 9, 10, 12));
		return new DetalleCitaDto(cita, "933731536", "andrea.salazar@gmail.com", new BigDecimal("150.00"), pago, true,
				null);
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
