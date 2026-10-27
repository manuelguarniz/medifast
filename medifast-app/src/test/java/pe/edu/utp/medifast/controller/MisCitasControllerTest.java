package pe.edu.utp.medifast.controller;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pe.edu.utp.medifast.config.SecurityConfig;
import pe.edu.utp.medifast.dao.MedicoDao;
import pe.edu.utp.medifast.dao.UsuarioDao;
import pe.edu.utp.medifast.dto.CitaPacienteDto;
import pe.edu.utp.medifast.dto.ConstanciaDto;
import pe.edu.utp.medifast.dto.MisCitasDto;
import pe.edu.utp.medifast.exception.AccesoDenegadoException;
import pe.edu.utp.medifast.model.Medico;
import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;
import pe.edu.utp.medifast.model.enums.EstadoPagoEnum;
import pe.edu.utp.medifast.model.enums.TipoCitaEnum;
import pe.edu.utp.medifast.security.EventosAutenticacion;
import pe.edu.utp.medifast.security.IntentosLoginService;
import pe.edu.utp.medifast.security.RedireccionPorRol;
import pe.edu.utp.medifast.security.UsuarioActual;
import pe.edu.utp.medifast.security.UsuarioDetailsService;
import pe.edu.utp.medifast.service.ConfirmacionPdfService;
import pe.edu.utp.medifast.service.ConstanciaService;
import pe.edu.utp.medifast.service.ReservaService;
import pe.edu.utp.medifast.util.Auditoria;

@WebMvcTest(MisCitasController.class)
@Import({ SecurityConfig.class, RedireccionPorRol.class, UsuarioDetailsService.class,
		IntentosLoginService.class, EventosAutenticacion.class })
class MisCitasControllerTest {

	private static final long ID_PACIENTE = 20L;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ReservaService reservaService;
	@MockitoBean
	private ConfirmacionPdfService confirmacionPdfService;
	@MockitoBean
	private ConstanciaService constanciaService;
	@MockitoBean
	private UsuarioActual usuarioActual;
	@MockitoBean
	private MedicoDao medicoDao;
	@MockitoBean
	private UsuarioDao usuarioDao;
	@MockitoBean
	private Auditoria auditoria;

	@BeforeEach
	void configurar() {
		when(usuarioActual.idPaciente(any())).thenReturn(ID_PACIENTE);
		Medico medico = mock(Medico.class);
		when(medico.getNombres()).thenReturn("Moisés");
		when(medico.getApellidos()).thenReturn("Barrantes Cabrera");
		when(medico.getEspecialidad()).thenReturn("Medicina Interna y Reumatología");
		when(medicoDao.findById(3L)).thenReturn(Optional.of(medico));
		when(medicoDao.findFirstByActivoTrueOrderByIdAsc()).thenReturn(Optional.of(medico));
	}

	@Test
	void muestraLaCitaConfirmadaConSusAccionesEnProximas() throws Exception {
		CitaPacienteDto confirmada = dto(124L, EstadoCitaEnum.PROGRAMADA, EstadoPagoEnum.POR_VALIDAR, false, 0, null);
		CitaPacienteDto cancelada = dto(117L, EstadoCitaEnum.CANCELADA, EstadoPagoEnum.RECHAZADO, false, 0,
				"Constancia ilegible.");
		when(reservaService.historial(ID_PACIENTE))
				.thenReturn(new MisCitasDto(Optional.of(confirmada), List.of(cancelada)));

		mockMvc.perform(get("/paciente/citas").with(user("70000001").roles("PACIENTE")))
				.andExpect(status().isOk())
				.andExpect(view().name("paciente/citas"))
				.andExpect(model().attribute("pestana", "proximas"))
				.andExpect(content().string(allOf(
						containsString("#000124"),
						containsString("Confirmada"),
						containsString("Nuevo · 60 min"),
						containsString("Dr. Moisés Barrantes Cabrera"),
						containsString("10:30–11:30 a. m."),
						containsString("S/ 150.00"),
						containsString("Por validar"),
						containsString("/paciente/citas/124/pdf"),
						containsString("/paciente/citas/124/constancia"),
						containsString("/paciente/confirmacion?citaId=124"),
						containsString("Solo puedes tener una cita activa"),
						not(containsString("Continuar pago")),
						not(containsString("Reservar cita <span")))));
	}

	@Test
	void ofreceContinuarPagoConContadorSiLaReservaSigueVigente() throws Exception {
		CitaPacienteDto pendiente = dto(124L, EstadoCitaEnum.PENDIENTE_PAGO, null, false, 768, null);
		when(reservaService.historial(ID_PACIENTE)).thenReturn(new MisCitasDto(Optional.of(pendiente), List.of()));

		mockMvc.perform(get("/paciente/citas").with(user("70000001").roles("PACIENTE")))
				.andExpect(status().isOk())
				.andExpect(content().string(allOf(
						containsString("Pago pendiente"),
						containsString("Tiempo para pagar"),
						containsString("data-segundos=\"768\""),
						containsString("12:48"),
						containsString("/paciente/pago?citaId=124"),
						not(containsString("Descargar PDF")),
						not(containsString("Ver detalle")),
						not(containsString("Cancelar cita")))));
	}

	@Test
	void sinCitasMuestraElEstadoVacioYPermiteReservar() throws Exception {
		when(reservaService.historial(ID_PACIENTE)).thenReturn(new MisCitasDto(Optional.empty(), List.of()));

		mockMvc.perform(get("/paciente/citas").with(user("70000001").roles("PACIENTE")))
				.andExpect(status().isOk())
				.andExpect(model().attribute("pestana", "proximas"))
				.andExpect(content().string(allOf(
						containsString("No tienes citas registradas"),
						containsString("Reserva una cita con el Dr. Moisés Barrantes Cabrera."),
						containsString("href=\"/paciente/reservar\""))));
	}

	@Test
	void sinCitaActivaAbreElHistorialConMotivosYAccionesPorEstado() throws Exception {
		List<CitaPacienteDto> historial = List.of(
				dto(130L, EstadoCitaEnum.PENDIENTE_PAGO, null, true, 0, null),
				dto(117L, EstadoCitaEnum.CANCELADA, EstadoPagoEnum.RECHAZADO, false, 0,
						"La constancia no corresponde al monto de la cita."),
				dto(109L, EstadoCitaEnum.CANCELADA, null, false, 0, "Tiempo de reserva vencido."),
				dto(101L, EstadoCitaEnum.ATENDIDA, EstadoPagoEnum.VALIDADO, false, 0, null));
		when(reservaService.historial(ID_PACIENTE)).thenReturn(new MisCitasDto(Optional.empty(), historial));

		mockMvc.perform(get("/paciente/citas").with(user("70000001").roles("PACIENTE")))
				.andExpect(status().isOk())
				.andExpect(model().attribute("pestana", "historial"))
				.andExpect(content().string(allOf(
						containsString("De la más reciente a la más antigua."),
						containsString("Vencida"),
						containsString("Rechazado"),
						containsString("Motivo: La constancia no corresponde al monto de la cita."),
						containsString("Motivo: Tiempo de reserva vencido."),
						containsString("Atendida"),
						containsString("Verificado"),
						containsString("/paciente/citas/117/constancia"),
						containsString("/paciente/citas/101/pdf"),
						not(containsString("/paciente/citas/109/constancia")),
						not(containsString("/paciente/citas/117/pdf")),
						not(containsString("/paciente/pago?citaId=130")))));
	}

	@Test
	void descargaLaConfirmacionEnPdf() throws Exception {
		when(confirmacionPdfService.generar(124L, ID_PACIENTE)).thenReturn("%PDF-1.7".getBytes());

		mockMvc.perform(get("/paciente/citas/124/pdf").with(user("70000001").roles("PACIENTE")))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/pdf"))
				.andExpect(header().string("Content-Disposition", containsString("attachment")))
				.andExpect(header().string("Content-Disposition", containsString("cita-000124.pdf")));
	}

	@Test
	void respondeProhibidoSiElPdfNoEsDelPaciente() throws Exception {
		when(confirmacionPdfService.generar(124L, ID_PACIENTE)).thenThrow(new AccesoDenegadoException("no"));

		mockMvc.perform(get("/paciente/citas/124/pdf").with(user("70000001").roles("PACIENTE")))
				.andExpect(status().isForbidden());
	}

	@Test
	void abreLaConstanciaEnLineaConSuTipo() throws Exception {
		when(constanciaService.paraPaciente(124L, ID_PACIENTE)).thenReturn(new ConstanciaDto(
				new ByteArrayResource("png".getBytes()), "image/png", "constancia-000124.png"));

		mockMvc.perform(get("/paciente/citas/124/constancia").with(user("70000001").roles("PACIENTE")))
				.andExpect(status().isOk())
				.andExpect(content().contentType("image/png"))
				.andExpect(header().string("Content-Disposition", containsString("inline")))
				.andExpect(content().bytes("png".getBytes()));
	}

	@Test
	void elMedicoNoAccedeAMisCitas() throws Exception {
		mockMvc.perform(get("/paciente/citas").with(user("10000001").roles("MEDICO")))
				.andExpect(status().isForbidden());
	}

	private static CitaPacienteDto dto(long id, EstadoCitaEnum estado, EstadoPagoEnum estadoPago, boolean vencida,
			long segundos, String observacion) {
		VistaCita cita = mock(VistaCita.class);
		when(cita.getIdCita()).thenReturn(id);
		when(cita.getIdMedico()).thenReturn(3L);
		when(cita.getTipoCita()).thenReturn(TipoCitaEnum.NUEVO);
		when(cita.getEstado()).thenReturn(estado);
		when(cita.getEstadoPago()).thenReturn(estadoPago);
		when(cita.getIdPago()).thenReturn(estadoPago == null ? null : id + 1000);
		when(cita.getFecha()).thenReturn(LocalDate.of(2026, 11, 11));
		when(cita.getHoraInicio()).thenReturn(LocalTime.of(10, 30));
		when(cita.getHoraFin()).thenReturn(LocalTime.of(11, 30));
		when(cita.getObservacion()).thenReturn(observacion);
		return new CitaPacienteDto(cita, new BigDecimal("150.00"), vencida, segundos);
	}
}
