package pe.edu.utp.medifast.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import pe.edu.utp.medifast.dao.CitaDao;
import pe.edu.utp.medifast.dao.HorarioSemanalDao;
import pe.edu.utp.medifast.dao.PagoDao;
import pe.edu.utp.medifast.dao.TarifaDao;
import pe.edu.utp.medifast.dao.VistaCitaDao;
import pe.edu.utp.medifast.dto.DetalleCitaDto;
import pe.edu.utp.medifast.exception.AccesoDenegadoException;
import pe.edu.utp.medifast.exception.AccionNoPermitidaException;
import pe.edu.utp.medifast.exception.PagoNoValidadoException;
import pe.edu.utp.medifast.exception.RecursoNoEncontradoException;
import pe.edu.utp.medifast.model.Cita;
import pe.edu.utp.medifast.model.CitaHorario;
import pe.edu.utp.medifast.model.Horario;
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

/** Acciones de P8 (RF08, RF13, RF14) y sus reglas RN-07, RN-13 y RN-14. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GestionCitaServiceImplAccionesTest {

	private static final long ID_MEDICO = 1L;
	private static final long ID_CITA = 124L;
	/** Miércoles 11 de noviembre de 2026, 11:45 en Lima: la cita de las 10:30 ya empezó. */
	private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-11T16:45:00Z"), ZoneId.of("America/Lima"));
	private static final LocalDate MIERCOLES = LocalDate.of(2026, 11, 11);

	@Mock
	private VistaCitaDao vistaCitaDao;
	@Mock
	private HorarioSemanalDao horarioSemanalDao;
	@Mock
	private CitaDao citaDao;
	@Mock
	private PagoDao pagoDao;
	@Mock
	private TarifaDao tarifaDao;
	@Mock
	private Auditoria auditoria;

	private GestionCitaServiceImpl servicio;
	private Cita cita;

	@BeforeEach
	void configurar() {
		servicio = new GestionCitaServiceImpl(vistaCitaDao, horarioSemanalDao, citaDao, pagoDao, tarifaDao,
				auditoria, RELOJ);
		cita = cita(ID_MEDICO, EstadoCitaEnum.PROGRAMADA);
		when(citaDao.findById(ID_CITA)).thenReturn(Optional.of(cita));
		vista(LocalTime.of(10, 30));
	}

	@Test
	void validarPagoLoDejaVerificadoConLaFechaYHora() {
		Pago pago = pago(EstadoPagoEnum.POR_VALIDAR);

		servicio.validarPago(ID_CITA, ID_MEDICO);

		assertThat(pago.getEstado()).isEqualTo(EstadoPagoEnum.VALIDADO);
		assertThat(pago.getValidadoEn()).isEqualTo(LocalDateTime.of(2026, 11, 11, 11, 45));
		verify(auditoria).registrar(Evento.PAGO_VALIDADO, "id_cita=124");
	}

	@Test
	void validarPagoSoloSobrePagosPorValidar() {
		pago(EstadoPagoEnum.VALIDADO);

		assertThatThrownBy(() -> servicio.validarPago(ID_CITA, ID_MEDICO))
				.isInstanceOf(AccionNoPermitidaException.class);
		verify(auditoria, never()).registrar(Evento.PAGO_VALIDADO, "id_cita=124");
	}

	@Test
	void validarPagoSinPagoNoSePermite() {
		when(pagoDao.findByCitaId(ID_CITA)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.validarPago(ID_CITA, ID_MEDICO))
				.isInstanceOf(AccionNoPermitidaException.class);
	}

	@Test
	void rechazarPagoCancelaLaCitaConElMotivoYLiberaLosBloques() {
		Pago pago = pago(EstadoPagoEnum.POR_VALIDAR);

		servicio.rechazarPago(ID_CITA, ID_MEDICO, "  La constancia no corresponde al monto de la cita.  ");

		assertThat(pago.getEstado()).isEqualTo(EstadoPagoEnum.RECHAZADO);
		assertThat(cita.getEstado()).isEqualTo(EstadoCitaEnum.CANCELADA);
		assertThat(cita.getObservacion()).isEqualTo("La constancia no corresponde al monto de la cita.");
		assertThat(cita.getCanceladaPor()).isEqualTo(CanceladaPorEnum.MEDICO);
		assertThat(cita.getBloques()).hasSize(2).noneMatch(CitaHorario::isActivo);
		verify(auditoria).registrar(Evento.PAGO_RECHAZADO, "id_cita=124");
	}

	@Test
	void rechazarYCancelarExigenElMotivo() {
		pago(EstadoPagoEnum.POR_VALIDAR);

		assertThatThrownBy(() -> servicio.rechazarPago(ID_CITA, ID_MEDICO, "   "))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("Ingresa el motivo.");
		assertThatThrownBy(() -> servicio.cancelar(ID_CITA, ID_MEDICO, null))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("Ingresa el motivo.");
		assertThatThrownBy(() -> servicio.cancelar(ID_CITA, ID_MEDICO, "x".repeat(501)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("El motivo no puede superar los 500 caracteres.");
		assertThat(cita.getEstado()).isEqualTo(EstadoCitaEnum.PROGRAMADA);
	}

	@Test
	void marcarAtendidaExigeElPagoValidado() {
		pago(EstadoPagoEnum.POR_VALIDAR);

		assertThatThrownBy(() -> servicio.marcarAtendida(ID_CITA, ID_MEDICO))
				.isInstanceOf(PagoNoValidadoException.class).hasMessage("Primero valida el pago.");
		assertThat(cita.getEstado()).isEqualTo(EstadoCitaEnum.PROGRAMADA);
	}

	@Test
	void marcarAtendidaConPagoValidadoYHoraAlcanzada() {
		pago(EstadoPagoEnum.VALIDADO);

		servicio.marcarAtendida(ID_CITA, ID_MEDICO);

		assertThat(cita.getEstado()).isEqualTo(EstadoCitaEnum.ATENDIDA);
		verify(auditoria).registrar(Evento.ASISTENCIA, "id_cita=124 estado=ATENDIDA");
	}

	@Test
	void asistenciaSoloDesdeLaHoraDeInicio() {
		pago(EstadoPagoEnum.VALIDADO);
		vista(LocalTime.of(12, 0));

		assertThatThrownBy(() -> servicio.marcarAtendida(ID_CITA, ID_MEDICO))
				.isInstanceOf(AccionNoPermitidaException.class).hasMessage("La cita aún no empieza.");
		assertThatThrownBy(() -> servicio.marcarNoAsistio(ID_CITA, ID_MEDICO))
				.isInstanceOf(AccionNoPermitidaException.class).hasMessage("La cita aún no empieza.");
		assertThat(cita.getEstado()).isEqualTo(EstadoCitaEnum.PROGRAMADA);
	}

	@Test
	void laHoraDeInicioExactaYaPermiteMarcar() {
		vista(LocalTime.of(11, 45));

		servicio.marcarNoAsistio(ID_CITA, ID_MEDICO);

		assertThat(cita.getEstado()).isEqualTo(EstadoCitaEnum.NO_ASISTIO);
	}

	@Test
	void noAsistioNoExigeElPago() {
		pago(EstadoPagoEnum.POR_VALIDAR);

		servicio.marcarNoAsistio(ID_CITA, ID_MEDICO);

		assertThat(cita.getEstado()).isEqualTo(EstadoCitaEnum.NO_ASISTIO);
		verify(auditoria).registrar(Evento.ASISTENCIA, "id_cita=124 estado=NO_ASISTIO");
	}

	@Test
	void asistenciaSoloSobreCitasConfirmadas() {
		cita.setEstado(EstadoCitaEnum.PENDIENTE_PAGO);

		assertThatThrownBy(() -> servicio.marcarNoAsistio(ID_CITA, ID_MEDICO))
				.isInstanceOf(AccionNoPermitidaException.class);
	}

	@Test
	void cancelarUnaCitaConPagoPendienteLiberaLosBloques() {
		cita.setEstado(EstadoCitaEnum.PENDIENTE_PAGO);

		servicio.cancelar(ID_CITA, ID_MEDICO, "El paciente pidió cancelar por teléfono.");

		assertThat(cita.getEstado()).isEqualTo(EstadoCitaEnum.CANCELADA);
		assertThat(cita.getObservacion()).isEqualTo("El paciente pidió cancelar por teléfono.");
		assertThat(cita.getCanceladaPor()).isEqualTo(CanceladaPorEnum.MEDICO);
		assertThat(cita.getBloques()).noneMatch(CitaHorario::isActivo);
		verify(auditoria).registrar(Evento.CANCELACION, "id_cita=124");
	}

	@Test
	void cancelarSoloCitasActivas() {
		cita.setEstado(EstadoCitaEnum.ATENDIDA);

		assertThatThrownBy(() -> servicio.cancelar(ID_CITA, ID_MEDICO, "Motivo"))
				.isInstanceOf(AccionNoPermitidaException.class);
		assertThat(cita.getBloques()).allMatch(CitaHorario::isActivo);
	}

	@Test
	void elMedicoSoloOperaSobreSusCitas() {
		Cita ajena = cita(2L, EstadoCitaEnum.PROGRAMADA);
		when(citaDao.findById(200L)).thenReturn(Optional.of(ajena));
		when(citaDao.findById(999L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.detalle(200L, ID_MEDICO)).isInstanceOf(AccesoDenegadoException.class);
		assertThatThrownBy(() -> servicio.cancelar(200L, ID_MEDICO, "Motivo"))
				.isInstanceOf(AccesoDenegadoException.class);
		assertThatThrownBy(() -> servicio.detalle(999L, ID_MEDICO))
				.isInstanceOf(RecursoNoEncontradoException.class);
		verify(auditoria, never()).registrar(any(Evento.class), anyString());
	}

	@Test
	void detalleConPagoPorValidarHabilitaValidarRechazarYNoAsistio() {
		pago(EstadoPagoEnum.POR_VALIDAR);

		DetalleCitaDto detalle = servicio.detalle(ID_CITA, ID_MEDICO);

		assertThat(detalle.monto()).isEqualByComparingTo("150.00");
		assertThat(detalle.celular()).isEqualTo("933731536");
		assertThat(detalle.iniciada()).isTrue();
		assertThat(detalle.pagoPorValidar()).isTrue();
		assertThat(detalle.puedeMarcarAtendida()).isFalse();
		assertThat(detalle.puedeMarcarNoAsistio()).isTrue();
		assertThat(detalle.puedeCancelar()).isTrue();
		assertThat(detalle.finalizada()).isFalse();
	}

	@Test
	void detalleSinPagoTomaElMontoDeLaTarifa() {
		cita.setEstado(EstadoCitaEnum.PENDIENTE_PAGO);
		when(pagoDao.findByCitaId(ID_CITA)).thenReturn(Optional.empty());
		Tarifa tarifa = mock(Tarifa.class);
		when(tarifa.getPrecio()).thenReturn(new BigDecimal("150.00"));
		when(tarifaDao.findByMedicoIdAndTipoCita(ID_MEDICO, TipoCitaEnum.NUEVO)).thenReturn(Optional.of(tarifa));
		vista(LocalTime.of(12, 0));

		DetalleCitaDto detalle = servicio.detalle(ID_CITA, ID_MEDICO);

		assertThat(detalle.monto()).isEqualByComparingTo("150.00");
		assertThat(detalle.pago()).isNull();
		assertThat(detalle.pagoPorValidar()).isFalse();
		assertThat(detalle.admiteAsistencia()).isFalse();
		assertThat(detalle.puedeCancelar()).isTrue();
	}

	@Test
	void detalleDeUnaCitaAtendidaNoTieneAcciones() {
		pago(EstadoPagoEnum.VALIDADO);
		cita.setEstado(EstadoCitaEnum.ATENDIDA);

		DetalleCitaDto detalle = servicio.detalle(ID_CITA, ID_MEDICO);

		assertThat(detalle.finalizada()).isTrue();
		assertThat(detalle.pagoPorValidar()).isFalse();
		assertThat(detalle.admiteAsistencia()).isFalse();
		assertThat(detalle.puedeCancelar()).isFalse();
	}

	/** Cita NUEVO de Andrea con 2 bloques activos. */
	private static Cita cita(long idMedico, EstadoCitaEnum estado) {
		Medico medico = mock(Medico.class);
		when(medico.getId()).thenReturn(idMedico);
		Paciente paciente = mock(Paciente.class);
		when(paciente.getCelular()).thenReturn("933731536");
		when(paciente.getCorreo()).thenReturn("andrea.salazar@gmail.com");
		Cita nueva = new Cita(paciente, medico, TipoCitaEnum.NUEVO, null);
		ReflectionTestUtils.setField(nueva, "id", ID_CITA);
		nueva.setEstado(estado);
		nueva.agregarBloque(mock(Horario.class));
		nueva.agregarBloque(mock(Horario.class));
		return nueva;
	}

	private Pago pago(EstadoPagoEnum estado) {
		Pago pago = new Pago(cita, new BigDecimal("150.00"), "25789222", "uploads/comprobantes/a.jpg", "image/jpeg",
				LocalDateTime.of(2026, 11, 9, 10, 12));
		pago.setEstado(estado);
		when(pagoDao.findByCitaId(ID_CITA)).thenReturn(Optional.of(pago));
		return pago;
	}

	private void vista(LocalTime inicio) {
		VistaCita vista = mock(VistaCita.class);
		when(vista.getIdCita()).thenReturn(ID_CITA);
		when(vista.getFecha()).thenReturn(MIERCOLES);
		when(vista.getHoraInicio()).thenReturn(inicio);
		when(vista.getHoraFin()).thenReturn(inicio.plusHours(1));
		when(vista.getEstado()).thenAnswer(invocacion -> cita.getEstado());
		when(vistaCitaDao.findById(ID_CITA)).thenReturn(Optional.of(vista));
	}
}
