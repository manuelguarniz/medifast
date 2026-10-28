package pe.edu.utp.medifast.controller;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pe.edu.utp.medifast.config.SecurityConfig;
import pe.edu.utp.medifast.dao.UsuarioDao;
import pe.edu.utp.medifast.dto.CitaPacienteDto;
import pe.edu.utp.medifast.dto.ConstanciaDto;
import pe.edu.utp.medifast.dto.PagoPendienteDto;
import pe.edu.utp.medifast.exception.AccesoDenegadoException;
import pe.edu.utp.medifast.exception.PagoInvalidoException;
import pe.edu.utp.medifast.exception.RecursoNoEncontradoException;
import pe.edu.utp.medifast.exception.ReservaVencidaException;
import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.model.enums.CanceladaPorEnum;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;
import pe.edu.utp.medifast.model.enums.EstadoPagoEnum;
import pe.edu.utp.medifast.model.enums.TipoCitaEnum;
import pe.edu.utp.medifast.security.EventosAutenticacion;
import pe.edu.utp.medifast.security.IntentosLoginService;
import pe.edu.utp.medifast.security.RedireccionPorRol;
import pe.edu.utp.medifast.security.UsuarioActual;
import pe.edu.utp.medifast.security.UsuarioDetailsService;
import pe.edu.utp.medifast.service.ConstanciaService;
import pe.edu.utp.medifast.service.PagoService;
import pe.edu.utp.medifast.service.ReservaService;
import pe.edu.utp.medifast.util.Auditoria;

@WebMvcTest(PagoController.class)
@Import({ SecurityConfig.class, RedireccionPorRol.class, UsuarioDetailsService.class,
		IntentosLoginService.class, EventosAutenticacion.class })
class PagoControllerTest {

	private static final long ID_CITA = 124L;
	private static final long ID_PACIENTE = 20L;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PagoService pagoService;
	@MockitoBean
	private UsuarioActual usuarioActual;
	@MockitoBean
	private ReservaService reservaService;
	@MockitoBean
	private ConstanciaService constanciaService;
	@MockitoBean
	private UsuarioDao usuarioDao;
	@MockitoBean
	private Auditoria auditoria;

	private final MockMultipartFile constancia = new MockMultipartFile("comprobante", "constancia-yape.jpg",
			"image/jpeg", new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF });

	@BeforeEach
	void configurar() {
		when(usuarioActual.idPaciente(any())).thenReturn(ID_PACIENTE);
	}

	@Test
	void muestraElMontoElContadorYLaLeyendaDeSimulacion() throws Exception {
		PagoPendienteDto pago = pago(EstadoCitaEnum.PENDIENTE_PAGO, 872, null);
		when(pagoService.paraPagar(ID_CITA, ID_PACIENTE)).thenReturn(pago);

		mockMvc.perform(get("/paciente/pago").param("citaId", "124").with(user("70000001").roles("PACIENTE")))
				.andExpect(status().isOk())
				.andExpect(view().name("paciente/pago"))
				.andExpect(content().string(allOf(
						containsString("Paga tu cita"),
						containsString("Mié 11 nov · 10:30–11:30 a. m. · Nuevo · Dr. Moisés Barrantes Cabrera"),
						containsString("Tu horario está bloqueado temporalmente para completar el pago"),
						containsString("data-segundos=\"872\""),
						containsString(">14:32<"),
						containsString("S/ 150.00"),
						containsString("QR de demostración"),
						containsString("Moisés Barrantes Cabrera"),
						containsString("999 888 777"),
						containsString("JPG, PNG o PDF hasta 5 MB"),
						containsString("N.º de operación (opcional)"),
						containsString("Enviar constancia"),
						containsString("Volver"),
						containsString("Simulación — no se realizó ningún cobro"),
						containsString("data-abierto=\"false\""))));
	}

	@Test
	void reservaVencidaAbreElModal() throws Exception {
		PagoPendienteDto pago = pago(EstadoCitaEnum.CANCELADA, 0, CanceladaPorEnum.SISTEMA);
		when(pagoService.paraPagar(ID_CITA, ID_PACIENTE)).thenReturn(pago);

		mockMvc.perform(get("/paciente/pago").param("citaId", "124").with(user("70000001").roles("PACIENTE")))
				.andExpect(status().isOk())
				.andExpect(content().string(allOf(
						containsString("data-abierto=\"true\""),
						containsString("Tiempo de reserva vencido"),
						containsString("Reservar otra cita"),
						containsString(">00:00<"))));
	}

	@Test
	void unaCitaYaPagadaVaALaConfirmacion() throws Exception {
		PagoPendienteDto pago = pago(EstadoCitaEnum.PROGRAMADA, 0, null);
		when(pagoService.paraPagar(ID_CITA, ID_PACIENTE)).thenReturn(pago);

		mockMvc.perform(get("/paciente/pago").param("citaId", "124").with(user("70000001").roles("PACIENTE")))
				.andExpect(redirectedUrl("/paciente/confirmacion?citaId=124"));
	}

	@Test
	void unaCitaCanceladaPorElMedicoVaAMisCitas() throws Exception {
		PagoPendienteDto pago = pago(EstadoCitaEnum.CANCELADA, 0, CanceladaPorEnum.MEDICO);
		when(pagoService.paraPagar(ID_CITA, ID_PACIENTE)).thenReturn(pago);

		mockMvc.perform(get("/paciente/pago").param("citaId", "124").with(user("70000001").roles("PACIENTE")))
				.andExpect(redirectedUrl("/paciente/citas"));
	}

	@Test
	void citaAjenaResponde403() throws Exception {
		when(pagoService.paraPagar(ID_CITA, ID_PACIENTE))
				.thenThrow(new AccesoDenegadoException("La cita no pertenece al paciente autenticado."));

		mockMvc.perform(get("/paciente/pago").param("citaId", "124").with(user("70000001").roles("PACIENTE")))
				.andExpect(status().isForbidden());
	}

	@Test
	void enviarLaConstanciaLlevaALaConfirmacion() throws Exception {
		mockMvc.perform(multipart("/paciente/pago").file(constancia).param("citaId", "124")
				.param("nroOperacion", "25789222").with(csrf()).with(user("70000001").roles("PACIENTE")))
				.andExpect(redirectedUrl("/paciente/confirmacion?citaId=124"))
				.andExpect(flash().attribute("constanciaEnviada", true));
	}

	@Test
	void erroresDeValidacionSeMuestranBajoCadaCampo() throws Exception {
		when(pagoService.registrarPago(eq(ID_CITA), eq(ID_PACIENTE), eq("2578-92a"), any()))
				.thenThrow(new PagoInvalidoException(Map.of(
						"comprobante", "Formato no permitido. Usa JPG, PNG o PDF.",
						"nroOperacion", "Ingresa solo números.")));
		PagoPendienteDto pago = pago(EstadoCitaEnum.PENDIENTE_PAGO, 375, null);
		when(pagoService.paraPagar(ID_CITA, ID_PACIENTE)).thenReturn(pago);

		mockMvc.perform(multipart("/paciente/pago").file(constancia).param("citaId", "124")
				.param("nroOperacion", "2578-92a").with(csrf()).with(user("70000001").roles("PACIENTE")))
				.andExpect(status().isOk())
				.andExpect(view().name("paciente/pago"))
				.andExpect(model().attribute("nroOperacion", "2578-92a"))
				.andExpect(content().string(allOf(
						containsString("Formato no permitido. Usa JPG, PNG o PDF."),
						containsString("Ingresa solo números."),
						containsString("pago-subida-error"),
						containsString("value=\"2578-92a\""),
						not(containsString("data-abierto=\"true\"")))));
	}

	@Test
	void plazoVencidoAlEnviarVuelveAlModal() throws Exception {
		when(pagoService.registrarPago(eq(ID_CITA), eq(ID_PACIENTE), any(), any()))
				.thenThrow(new ReservaVencidaException());

		mockMvc.perform(multipart("/paciente/pago").file(constancia).param("citaId", "124").with(csrf())
				.with(user("70000001").roles("PACIENTE")))
				.andExpect(redirectedUrl("/paciente/pago?citaId=124"));
	}

	@Test
	void elMedicoNoAccedeAlPago() throws Exception {
		mockMvc.perform(get("/paciente/pago").param("citaId", "124").with(user("10000001").roles("MEDICO")))
				.andExpect(status().isForbidden());
	}

	@Test
	void confirmacionDesdeElPagoMuestraCitaReservadaYLaAlerta() throws Exception {
		CitaPacienteDto cita = citaPaciente(EstadoCitaEnum.PROGRAMADA, EstadoPagoEnum.POR_VALIDAR, null, false, 0);
		when(reservaService.cita(ID_CITA, ID_PACIENTE)).thenReturn(cita);

		mockMvc.perform(get("/paciente/confirmacion").param("citaId", "124").flashAttr("constanciaEnviada", true)
				.with(user("70000001").roles("PACIENTE")))
				.andExpect(status().isOk())
				.andExpect(view().name("paciente/confirmacion"))
				.andExpect(content().string(allOf(
						containsString("Cita <span>reservada</span>"),
						containsString("¡Cita reservada!"),
						containsString("Tu constancia fue recibida y el médico validará el pago."),
						containsString("#000124"),
						containsString("Andrea Sofía Salazar Pérez"),
						containsString("Dr. Moisés Barrantes Cabrera"),
						containsString("Nuevo · 60 min"),
						containsString("Mié, 11 nov · 10:30–11:30 a. m."),
						containsString("S/ 150.00"),
						containsString("Confirmada"),
						containsString("Por validar"),
						containsString("/paciente/citas/124/pdf"),
						containsString("Ir a mis citas"),
						not(containsString("Ver constancia")),
						not(containsString("correo")))));
	}

	@Test
	void detalleDesdeMisCitasMuestraLaConstanciaSinAlerta() throws Exception {
		CitaPacienteDto cita = citaPaciente(EstadoCitaEnum.ATENDIDA, EstadoPagoEnum.VALIDADO, null, false, 0);
		when(reservaService.cita(ID_CITA, ID_PACIENTE)).thenReturn(cita);
		when(constanciaService.paraPaciente(ID_CITA, ID_PACIENTE)).thenReturn(
				new ConstanciaDto(new ByteArrayResource(new byte[] { 1 }), "image/jpeg", "constancia-000124.jpg"));

		mockMvc.perform(get("/paciente/confirmacion").param("citaId", "124").with(user("70000001").roles("PACIENTE")))
				.andExpect(status().isOk())
				.andExpect(content().string(allOf(
						containsString("Detalle <span>de la cita</span>"),
						containsString("Atendida"),
						containsString("Verificado"),
						containsString("constancia-000124.jpg"),
						containsString("/paciente/citas/124/constancia"),
						containsString("Volver a mis citas"),
						containsString("Descargar PDF"),
						not(containsString("¡Cita reservada!")))));
	}

	@Test
	void detalleDeUnaCitaCanceladaMuestraElMotivoSinPdf() throws Exception {
		CitaPacienteDto cita = citaPaciente(EstadoCitaEnum.CANCELADA, null, "Tiempo de reserva vencido", false, 0);
		when(reservaService.cita(ID_CITA, ID_PACIENTE)).thenReturn(cita);

		mockMvc.perform(get("/paciente/confirmacion").param("citaId", "124").with(user("70000001").roles("PACIENTE")))
				.andExpect(status().isOk())
				.andExpect(content().string(allOf(
						containsString("Cancelada"),
						containsString("Tiempo de reserva vencido"),
						not(containsString("Descargar PDF")),
						not(containsString("Ver constancia")))));
	}

	@Test
	void constanciaSinArchivoSeMuestraComoNoDisponible() throws Exception {
		CitaPacienteDto cita = citaPaciente(EstadoCitaEnum.PROGRAMADA, EstadoPagoEnum.POR_VALIDAR, null, false, 0);
		when(reservaService.cita(ID_CITA, ID_PACIENTE)).thenReturn(cita);
		when(constanciaService.paraPaciente(ID_CITA, ID_PACIENTE))
				.thenThrow(new RecursoNoEncontradoException("No se encontró el archivo de la constancia."));

		mockMvc.perform(get("/paciente/confirmacion").param("citaId", "124").with(user("70000001").roles("PACIENTE")))
				.andExpect(status().isOk())
				.andExpect(content().string(allOf(containsString("No disponible"), not(containsString("Ver constancia")))));
	}

	@Test
	void unaReservaQueAunEsperaElPagoVuelveAlPago() throws Exception {
		CitaPacienteDto cita = citaPaciente(EstadoCitaEnum.PENDIENTE_PAGO, null, null, false, 300);
		when(reservaService.cita(ID_CITA, ID_PACIENTE)).thenReturn(cita);

		mockMvc.perform(get("/paciente/confirmacion").param("citaId", "124").with(user("70000001").roles("PACIENTE")))
				.andExpect(redirectedUrl("/paciente/pago?citaId=124"));
	}

	@Test
	void unaReservaVencidaSeMuestraComoVencida() throws Exception {
		CitaPacienteDto cita = citaPaciente(EstadoCitaEnum.PENDIENTE_PAGO, null, null, true, 0);
		when(reservaService.cita(ID_CITA, ID_PACIENTE)).thenReturn(cita);

		mockMvc.perform(get("/paciente/confirmacion").param("citaId", "124").with(user("70000001").roles("PACIENTE")))
				.andExpect(status().isOk())
				.andExpect(content().string(allOf(containsString("Vencida"), not(containsString("Descargar PDF")))));
	}

	@Test
	void confirmacionDeCitaAjenaResponde403() throws Exception {
		when(reservaService.cita(ID_CITA, ID_PACIENTE))
				.thenThrow(new AccesoDenegadoException("La cita no pertenece al paciente autenticado."));

		mockMvc.perform(get("/paciente/confirmacion").param("citaId", "124").with(user("70000001").roles("PACIENTE")))
				.andExpect(status().isForbidden());
	}

	private static CitaPacienteDto citaPaciente(EstadoCitaEnum estado, EstadoPagoEnum estadoPago, String observacion,
			boolean vencida, long segundos) {
		VistaCita cita = mock(VistaCita.class);
		when(cita.getIdCita()).thenReturn(ID_CITA);
		when(cita.getPaciente()).thenReturn("Andrea Sofía Salazar Pérez");
		when(cita.getMedico()).thenReturn("Moisés Barrantes Cabrera");
		when(cita.getFecha()).thenReturn(LocalDate.of(2026, 11, 11));
		when(cita.getHoraInicio()).thenReturn(LocalTime.of(10, 30));
		when(cita.getHoraFin()).thenReturn(LocalTime.of(11, 30));
		when(cita.getTipoCita()).thenReturn(TipoCitaEnum.NUEVO);
		when(cita.getEstado()).thenReturn(estado);
		when(cita.getEstadoPago()).thenReturn(estadoPago);
		when(cita.getIdPago()).thenReturn(estadoPago == null ? null : 500L);
		when(cita.getObservacion()).thenReturn(observacion);
		return new CitaPacienteDto(cita, new BigDecimal("150.00"), vencida, segundos);
	}

	/** Cita #000124 de Andrea del diseño aprobado de P3. */
	private static PagoPendienteDto pago(EstadoCitaEnum estado, long segundos, CanceladaPorEnum canceladaPor) {
		VistaCita cita = mock(VistaCita.class);
		when(cita.getIdCita()).thenReturn(ID_CITA);
		when(cita.getFecha()).thenReturn(LocalDate.of(2026, 11, 11));
		when(cita.getHoraInicio()).thenReturn(LocalTime.of(10, 30));
		when(cita.getHoraFin()).thenReturn(LocalTime.of(11, 30));
		when(cita.getTipoCita()).thenReturn(TipoCitaEnum.NUEVO);
		when(cita.getMedico()).thenReturn("Moisés Barrantes Cabrera");
		when(cita.getEstado()).thenReturn(estado);
		when(cita.getCanceladaPor()).thenReturn(canceladaPor);
		return new PagoPendienteDto(cita, new BigDecimal("150.00"), segundos);
	}
}
