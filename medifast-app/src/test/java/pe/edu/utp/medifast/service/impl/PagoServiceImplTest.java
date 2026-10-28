package pe.edu.utp.medifast.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import pe.edu.utp.medifast.dao.CitaDao;
import pe.edu.utp.medifast.dao.PagoDao;
import pe.edu.utp.medifast.dao.TarifaDao;
import pe.edu.utp.medifast.dao.VistaCitaDao;
import pe.edu.utp.medifast.dto.PagoPendienteDto;
import pe.edu.utp.medifast.exception.AccesoDenegadoException;
import pe.edu.utp.medifast.exception.AccionNoPermitidaException;
import pe.edu.utp.medifast.exception.PagoInvalidoException;
import pe.edu.utp.medifast.exception.ReservaVencidaException;
import pe.edu.utp.medifast.model.Cita;
import pe.edu.utp.medifast.model.Medico;
import pe.edu.utp.medifast.model.Paciente;
import pe.edu.utp.medifast.model.Pago;
import pe.edu.utp.medifast.model.Tarifa;
import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.model.enums.CanceladaPorEnum;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;
import pe.edu.utp.medifast.model.enums.EstadoPagoEnum;
import pe.edu.utp.medifast.model.enums.TipoCitaEnum;
import pe.edu.utp.medifast.util.Auditoria;
import pe.edu.utp.medifast.util.Auditoria.Evento;

/** P3: RF03, RN-04, RN-11 y RNF06. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PagoServiceImplTest {

	private static final long ID_CITA = 124L;
	private static final long ID_PACIENTE = 20L;
	private static final long ID_MEDICO = 1L;
	/** 10:20 del lunes 9 de noviembre de 2026 en Lima; la reserva vence a las 10:30. */
	private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-09T15:20:00Z"), ZoneId.of("America/Lima"));
	private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 9, 10, 20);
	private static final byte[] JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F' };
	private static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0 };
	private static final byte[] PDF = "%PDF-1.7\n".getBytes();

	@TempDir
	private Path carpeta;
	@Mock
	private CitaDao citaDao;
	@Mock
	private PagoDao pagoDao;
	@Mock
	private TarifaDao tarifaDao;
	@Mock
	private VistaCitaDao vistaCitaDao;
	@Mock
	private Auditoria auditoria;

	private PagoServiceImpl servicio;
	private Cita cita;

	@BeforeEach
	void configurar() {
		servicio = new PagoServiceImpl(citaDao, pagoDao, tarifaDao, vistaCitaDao, auditoria, RELOJ,
				carpeta.toString());
		Paciente paciente = mock(Paciente.class);
		when(paciente.getId()).thenReturn(ID_PACIENTE);
		Medico medico = mock(Medico.class);
		when(medico.getId()).thenReturn(ID_MEDICO);
		cita = new Cita(paciente, medico, TipoCitaEnum.NUEVO, AHORA.plusMinutes(10));
		ReflectionTestUtils.setField(cita, "id", ID_CITA);
		when(citaDao.findParaPagarById(ID_CITA)).thenReturn(Optional.of(cita));
		when(pagoDao.findByCitaId(ID_CITA)).thenReturn(Optional.empty());
		when(pagoDao.save(any(Pago.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
		Tarifa tarifa = mock(Tarifa.class);
		when(tarifa.getPrecio()).thenReturn(new BigDecimal("150.00"));
		when(tarifaDao.findByMedicoIdAndTipoCita(ID_MEDICO, TipoCitaEnum.NUEVO)).thenReturn(Optional.of(tarifa));
	}

	@Test
	void registraElPagoPorValidarYConfirmaLaCita() throws Exception {
		Pago pago = servicio.registrarPago(ID_CITA, ID_PACIENTE, " 25789222 ", archivo("captura.jpg", JPEG));

		assertThat(pago.getEstado()).isEqualTo(EstadoPagoEnum.POR_VALIDAR);
		assertThat(pago.getMonto()).isEqualByComparingTo("150.00");
		assertThat(pago.getNroOperacion()).isEqualTo("25789222");
		assertThat(pago.getComprobanteTipo()).isEqualTo("image/jpeg");
		assertThat(pago.getFechaPago()).isEqualTo(AHORA);
		assertThat(cita.getEstado()).isEqualTo(EstadoCitaEnum.PROGRAMADA);
		verify(auditoria).registrar(Evento.CONSTANCIA, "id_cita=124");
	}

	@Test
	void guardaElArchivoConNombreUuidYExtensionSegunSuContenido() throws Exception {
		Pago pago = servicio.registrarPago(ID_CITA, ID_PACIENTE, null, archivo("../../constancia.jpg", PNG));

		Path guardado = Path.of(pago.getComprobanteRuta());
		assertThat(guardado.getParent()).isEqualTo(carpeta);
		assertThat(guardado.getFileName().toString()).matches("[0-9a-f-]{36}\\.png");
		assertThat(Files.readAllBytes(guardado)).isEqualTo(PNG);
		assertThat(pago.getNroOperacion()).isNull();
	}

	@Test
	void aceptaPdf() throws Exception {
		Pago pago = servicio.registrarPago(ID_CITA, ID_PACIENTE, "", archivo("constancia.pdf", PDF));

		assertThat(pago.getComprobanteTipo()).isEqualTo("application/pdf");
	}

	@Test
	void exigeLaConstancia() {
		assertThatThrownBy(() -> servicio.registrarPago(ID_CITA, ID_PACIENTE, null, null))
				.isInstanceOf(PagoInvalidoException.class)
				.extracting(e -> ((PagoInvalidoException) e).getErrores())
				.isEqualTo(Map.of("comprobante", "Adjunta la constancia de pago."));
		assertThat(cita.getEstado()).isEqualTo(EstadoCitaEnum.PENDIENTE_PAGO);
	}

	@Test
	void rechazaUnArchivoQueSoloDiceSerImagen() {
		MockMultipartFile falso = new MockMultipartFile("comprobante", "virus.jpg", "image/jpeg",
				"MZ ejecutable".getBytes());

		assertThatThrownBy(() -> servicio.registrarPago(ID_CITA, ID_PACIENTE, "2578-92a", falso))
				.isInstanceOf(PagoInvalidoException.class)
				.extracting(e -> ((PagoInvalidoException) e).getErrores())
				.isEqualTo(Map.of("comprobante", "Formato no permitido. Usa JPG, PNG o PDF.",
						"nroOperacion", "Ingresa solo números."));
		verify(pagoDao, never()).save(any());
	}

	@Test
	void rechazaArchivosDeMasDe5Mb() {
		byte[] grande = new byte[5 * 1024 * 1024 + 1];
		System.arraycopy(JPEG, 0, grande, 0, JPEG.length);

		assertThatThrownBy(() -> servicio.registrarPago(ID_CITA, ID_PACIENTE, null, archivo("grande.jpg", grande)))
				.isInstanceOf(PagoInvalidoException.class)
				.hasMessage("El archivo supera los 5 MB.");
	}

	@Test
	void unaReservaVencidaNoAdmiteLaConstancia() {
		cita.setExpiraEn(AHORA);

		assertThatThrownBy(() -> servicio.registrarPago(ID_CITA, ID_PACIENTE, null, archivo("c.jpg", JPEG)))
				.isInstanceOf(ReservaVencidaException.class);
		assertThat(cita.getEstado()).isEqualTo(EstadoCitaEnum.PENDIENTE_PAGO);
	}

	@Test
	void unaReservaCanceladaPorElSistemaCuentaComoVencida() {
		cita.setEstado(EstadoCitaEnum.CANCELADA);
		cita.setCanceladaPor(CanceladaPorEnum.SISTEMA);

		assertThatThrownBy(() -> servicio.registrarPago(ID_CITA, ID_PACIENTE, null, archivo("c.jpg", JPEG)))
				.isInstanceOf(ReservaVencidaException.class);
	}

	@Test
	void unaCitaYaConfirmadaNoSePagaDosVeces() {
		cita.setEstado(EstadoCitaEnum.PROGRAMADA);

		assertThatThrownBy(() -> servicio.registrarPago(ID_CITA, ID_PACIENTE, null, archivo("c.jpg", JPEG)))
				.isInstanceOf(AccionNoPermitidaException.class);
	}

	@Test
	void soloElDuenoPaga() {
		assertThatThrownBy(() -> servicio.registrarPago(ID_CITA, 99L, null, archivo("c.jpg", JPEG)))
				.isInstanceOf(AccesoDenegadoException.class);
	}

	@Test
	void paraPagarCalculaElTiempoRestanteYLaTarifa() {
		VistaCita vista = vista(EstadoCitaEnum.PENDIENTE_PAGO, AHORA.plusMinutes(9).plusSeconds(47), null);

		PagoPendienteDto pago = servicio.paraPagar(ID_CITA, ID_PACIENTE);

		assertThat(pago.cita()).isSameAs(vista);
		assertThat(pago.segundosRestantes()).isEqualTo(587);
		assertThat(pago.monto()).isEqualByComparingTo("150.00");
		assertThat(pago.pendiente()).isTrue();
		assertThat(pago.vencida()).isFalse();
	}

	@Test
	void paraPagarDetectaLaReservaVencida() {
		vista(EstadoCitaEnum.PENDIENTE_PAGO, AHORA.minusSeconds(5), null);

		PagoPendienteDto pago = servicio.paraPagar(ID_CITA, ID_PACIENTE);

		assertThat(pago.segundosRestantes()).isZero();
		assertThat(pago.vencida()).isTrue();
		assertThat(pago.pendiente()).isFalse();
	}

	@Test
	void paraPagarNoMuestraCitasAjenas() {
		vista(EstadoCitaEnum.PENDIENTE_PAGO, AHORA.plusMinutes(5), null);

		assertThatThrownBy(() -> servicio.paraPagar(ID_CITA, 99L)).isInstanceOf(AccesoDenegadoException.class);
	}

	private VistaCita vista(EstadoCitaEnum estado, LocalDateTime expiraEn, CanceladaPorEnum canceladaPor) {
		VistaCita vista = mock(VistaCita.class);
		when(vista.getIdCita()).thenReturn(ID_CITA);
		when(vista.getIdPaciente()).thenReturn(ID_PACIENTE);
		when(vista.getIdMedico()).thenReturn(ID_MEDICO);
		when(vista.getTipoCita()).thenReturn(TipoCitaEnum.NUEVO);
		when(vista.getEstado()).thenReturn(estado);
		when(vista.getExpiraEn()).thenReturn(expiraEn);
		when(vista.getCanceladaPor()).thenReturn(canceladaPor);
		when(vistaCitaDao.findById(ID_CITA)).thenReturn(Optional.of(vista));
		return vista;
	}

	private static MockMultipartFile archivo(String nombre, byte[] contenido) {
		return new MockMultipartFile("comprobante", nombre, "application/octet-stream", contenido);
	}
}
