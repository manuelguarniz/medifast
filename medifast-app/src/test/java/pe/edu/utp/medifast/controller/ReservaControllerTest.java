package pe.edu.utp.medifast.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pe.edu.utp.medifast.config.SecurityConfig;
import pe.edu.utp.medifast.dao.HorarioDao;
import pe.edu.utp.medifast.dao.MedicoDao;
import pe.edu.utp.medifast.dao.UsuarioDao;
import pe.edu.utp.medifast.dto.BloqueHorarioDto;
import pe.edu.utp.medifast.dto.DisponibilidadDto;
import pe.edu.utp.medifast.dto.InicioDisponibleDto;
import pe.edu.utp.medifast.exception.HorarioNoDisponibleException;
import pe.edu.utp.medifast.model.Cita;
import pe.edu.utp.medifast.model.Consultorio;
import pe.edu.utp.medifast.model.Horario;
import pe.edu.utp.medifast.model.Medico;
import pe.edu.utp.medifast.model.Paciente;
import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;
import pe.edu.utp.medifast.model.enums.TipoCitaEnum;
import pe.edu.utp.medifast.security.EventosAutenticacion;
import pe.edu.utp.medifast.security.IntentosLoginService;
import pe.edu.utp.medifast.security.RedireccionPorRol;
import pe.edu.utp.medifast.security.UsuarioActual;
import pe.edu.utp.medifast.security.UsuarioDetailsService;
import pe.edu.utp.medifast.service.ReservaService;
import pe.edu.utp.medifast.util.Auditoria;

@WebMvcTest(ReservaController.class)
@Import({ SecurityConfig.class, RedireccionPorRol.class, UsuarioDetailsService.class,
		IntentosLoginService.class, EventosAutenticacion.class })
class ReservaControllerTest {

	private static final LocalDate FECHA =
			LocalDate.now(ZoneId.of("America/Lima")).plusDays(2);

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ReservaService reservaService;
	@MockitoBean
	private UsuarioActual usuarioActual;
	@MockitoBean
	private MedicoDao medicoDao;
	@MockitoBean
	private HorarioDao horarioDao;
	@MockitoBean
	private UsuarioDao usuarioDao;
	@MockitoBean
	private Auditoria auditoria;

	private Medico medico;

	@BeforeEach
	void configurarDatos() {
		Consultorio consultorio = mock(Consultorio.class);
		when(consultorio.getDiasAnticipacionMin()).thenReturn(1);
		when(consultorio.getDiasAnticipacionMax()).thenReturn(30);
		when(consultorio.getMinutosBloque()).thenReturn(30);
		medico = mock(Medico.class);
		when(medico.getId()).thenReturn(1L);
		when(medico.getNombres()).thenReturn("Moisés");
		when(medico.getApellidos()).thenReturn("Barrantes Cabrera");
		when(medico.getEspecialidad()).thenReturn("Medicina Interna y Reumatología");
		when(medico.getConsultorio()).thenReturn(consultorio);
		when(medicoDao.findFirstByActivoTrueOrderByIdAsc()).thenReturn(Optional.of(medico));
		Paciente paciente = mock(Paciente.class);
		when(paciente.getId()).thenReturn(20L);
		when(paciente.getNombres()).thenReturn("Ana María");
		when(paciente.getApellidos()).thenReturn("Salazar Rojas");
		when(usuarioActual.paciente(any())).thenReturn(paciente);
		when(usuarioActual.idPaciente(any())).thenReturn(20L);

		DisponibilidadDto disponibilidad = new DisponibilidadDto(TipoCitaEnum.NUEVO, 60,
				new BigDecimal("150.00"), FECHA.minusDays(1), FECHA.plusDays(28), FECHA,
				List.of(new InicioDisponibleDto(100L, java.time.LocalTime.of(10, 30),
						java.time.LocalTime.of(11, 30))),
				List.of(new BloqueHorarioDto(100L, java.time.LocalTime.of(10, 30),
						java.time.LocalTime.of(11, 0), true, false, false),
						new BloqueHorarioDto(101L, java.time.LocalTime.of(11, 0),
								java.time.LocalTime.of(11, 30), true, false, false)),
				Optional.empty());
		when(reservaService.disponibilidad(20L, 1L, FECHA)).thenReturn(disponibilidad);
		Horario horario = mock(Horario.class);
		when(horario.getFecha()).thenReturn(FECHA);
		when(horarioDao.findAllByMedicoIdAndFechaBetween(eq(1L), any(), any())).thenReturn(List.of(horario));
	}

	private void conCitaActiva(EstadoCitaEnum estado, LocalDateTime expiraEn) {
		VistaCita cita = mock(VistaCita.class);
		when(cita.getIdCita()).thenReturn(44L);
		when(cita.getEstado()).thenReturn(estado);
		when(cita.getExpiraEn()).thenReturn(expiraEn);
		when(cita.getFecha()).thenReturn(FECHA);
		when(cita.getHoraInicio()).thenReturn(LocalTime.of(10, 30));
		when(cita.getHoraFin()).thenReturn(LocalTime.of(11, 30));
		when(cita.getTipoCita()).thenReturn(TipoCitaEnum.NUEVO);
		DisponibilidadDto disponibilidad = new DisponibilidadDto(TipoCitaEnum.NUEVO, 60,
				new BigDecimal("150.00"), FECHA.minusDays(1), FECHA.plusDays(28), FECHA, List.of(), List.of(),
				Optional.of(cita));
		when(reservaService.disponibilidad(20L, 1L, FECHA)).thenReturn(disponibilidad);
	}

	@Test
	void muestraComoNoDisponibleElBloqueQueNoPuedeIniciarUnaCita() throws Exception {
		mockMvc.perform(get("/paciente/reservar").param("fecha", FECHA.toString())
				.with(user("70000001").roles("PACIENTE")))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.matchesPattern(
						"(?s).*class=\"reserva-slot\\s+unavailable\">\\s*<input[^>]*value=\"101\".*")));
	}

	@Test
	void muestraElRangoSeleccionadoConUnSoloPeriodo() throws Exception {
		mockMvc.perform(get("/paciente/reservar").param("fecha", FECHA.toString()).param("horarioId", "100")
				.with(user("70000001").roles("PACIENTE")))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("10:30 – 11:30 a. m.")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("10:30–11:30")));
	}

	@Test
	void permiteContinuarElPagoDeUnaCitaPendienteVigente() throws Exception {
		conCitaActiva(EstadoCitaEnum.PENDIENTE_PAGO, LocalDateTime.now(ZoneId.of("America/Lima")).plusMinutes(10));

		mockMvc.perform(get("/paciente/reservar").param("fecha", FECHA.toString())
				.with(user("70000001").roles("PACIENTE")))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Ya tienes una cita activa.")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("reserva-status-pendiente")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("/paciente/pago?citaId=44")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("reserva-countdown")));
	}

	@Test
	void muestraVerMisCitasSiLaCitaActivaEstaConfirmada() throws Exception {
		conCitaActiva(EstadoCitaEnum.PROGRAMADA, null);

		mockMvc.perform(get("/paciente/reservar").param("fecha", FECHA.toString())
				.with(user("70000001").roles("PACIENTE")))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("reserva-status-confirmada")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Ver mis citas")))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("/paciente/pago?citaId=44"))));
	}

	@Test
	void muestraReservaConHorariosYResumenEnEspanol() throws Exception {
		mockMvc.perform(get("/paciente/reservar").param("fecha", FECHA.toString())
				.with(user("70000001").roles("PACIENTE")))
				.andExpect(status().isOk())
				.andExpect(view().name("paciente/reservar"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Reserva tu cita")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Hola, ")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("<span>Ana</span>")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Ana María Salazar Rojas")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("AS")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Paciente nuevo")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("10:30")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Continuar al pago")));
	}

	@Test
	void muestraTarifaYDuracionDeControlSegunLaDisponibilidad() throws Exception {
		DisponibilidadDto control = new DisponibilidadDto(TipoCitaEnum.CONTROL, 30,
				new BigDecimal("80.00"), FECHA.minusDays(1), FECHA.plusDays(28), FECHA,
				List.of(new InicioDisponibleDto(100L, java.time.LocalTime.of(10, 30),
						java.time.LocalTime.of(11, 0))),
				List.of(new BloqueHorarioDto(100L, java.time.LocalTime.of(10, 30),
						java.time.LocalTime.of(11, 0), true, false, false)),
				Optional.empty());
		when(reservaService.disponibilidad(20L, 1L, FECHA)).thenReturn(control);

		mockMvc.perform(get("/paciente/reservar").param("fecha", FECHA.toString())
				.with(user("70000001").roles("PACIENTE")))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Paciente de control")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Control · 30 min")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("S/ 80.00")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("30 minutos")));
	}

	@Test
	void reservaElHorarioYRedirigeAlFlujoDePago() throws Exception {
		Cita cita = mock(Cita.class);
		when(cita.getId()).thenReturn(44L);
		when(reservaService.reservar(20L, 100L)).thenReturn(cita);

		mockMvc.perform(post("/paciente/reservar").with(user("70000001").roles("PACIENTE")).with(csrf())
				.param("fecha", FECHA.toString()).param("idHorario", "100"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/paciente/pago?citaId=44"));

		verify(reservaService).reservar(20L, 100L);
	}

	@Test
	void informaSiElHorarioSeOcupaAntesDeConfirmar() throws Exception {
		when(reservaService.reservar(20L, 100L)).thenThrow(new HorarioNoDisponibleException());

		mockMvc.perform(post("/paciente/reservar").with(user("70000001").roles("PACIENTE")).with(csrf())
				.param("fecha", FECHA.toString()).param("idHorario", "100"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/paciente/reservar?fecha=" + FECHA + "&error=horario"));
	}
}
