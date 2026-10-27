package pe.edu.utp.medifast.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import pe.edu.utp.medifast.dao.MedicoDao;
import pe.edu.utp.medifast.dao.VistaCitaDao;
import pe.edu.utp.medifast.exception.AccesoDenegadoException;
import pe.edu.utp.medifast.model.Medico;
import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;
import pe.edu.utp.medifast.model.enums.EstadoPagoEnum;
import pe.edu.utp.medifast.model.enums.TipoCitaEnum;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConfirmacionPdfServiceImplTest {

	private static final long ID_CITA = 124L;
	private static final long ID_PACIENTE = 20L;
	private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-09T15:00:00Z"), ZoneId.of("America/Lima"));

	@Mock
	private VistaCitaDao vistaCitaDao;
	@Mock
	private MedicoDao medicoDao;
	@Mock
	private VistaCita cita;
	@Mock
	private Medico medico;

	private ConfirmacionPdfServiceImpl servicio;

	@BeforeEach
	void configurar() {
		servicio = new ConfirmacionPdfServiceImpl(vistaCitaDao, medicoDao, RELOJ);
		when(cita.getIdCita()).thenReturn(ID_CITA);
		when(cita.getIdPaciente()).thenReturn(ID_PACIENTE);
		when(cita.getIdMedico()).thenReturn(3L);
		when(cita.getPaciente()).thenReturn("Andrea Sofía Salazar Pérez");
		when(cita.getMedico()).thenReturn("Moisés Barrantes Cabrera");
		when(cita.getFecha()).thenReturn(LocalDate.of(2026, 11, 11));
		when(cita.getHoraInicio()).thenReturn(LocalTime.of(10, 30));
		when(cita.getHoraFin()).thenReturn(LocalTime.of(11, 30));
		when(cita.getTipoCita()).thenReturn(TipoCitaEnum.NUEVO);
		when(cita.getMonto()).thenReturn(new BigDecimal("150.00"));
		when(cita.getEstadoPago()).thenReturn(EstadoPagoEnum.POR_VALIDAR);
		when(vistaCitaDao.findById(ID_CITA)).thenReturn(Optional.of(cita));
		when(medico.getEspecialidad()).thenReturn("Medicina Interna y Reumatología");
		when(medicoDao.findById(3L)).thenReturn(Optional.of(medico));
	}

	@ParameterizedTest
	@EnumSource(value = EstadoCitaEnum.class, names = { "PROGRAMADA", "ATENDIDA" })
	void generaUnPdfParaCitasConfirmadasOAtendidas(EstadoCitaEnum estado) {
		when(cita.getEstado()).thenReturn(estado);

		byte[] pdf = servicio.generar(ID_CITA, ID_PACIENTE);

		assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
	}

	@ParameterizedTest
	@EnumSource(value = EstadoCitaEnum.class, names = { "PENDIENTE_PAGO", "NO_ASISTIO", "CANCELADA" })
	void niegaElPdfEnOtrosEstados(EstadoCitaEnum estado) {
		when(cita.getEstado()).thenReturn(estado);

		assertThatThrownBy(() -> servicio.generar(ID_CITA, ID_PACIENTE)).isInstanceOf(AccesoDenegadoException.class);
	}

	@Test
	void niegaElPdfDeUnaCitaDeOtroPaciente() {
		when(cita.getEstado()).thenReturn(EstadoCitaEnum.PROGRAMADA);

		assertThatThrownBy(() -> servicio.generar(ID_CITA, 99L)).isInstanceOf(AccesoDenegadoException.class);
	}
}
