package pe.edu.utp.medifast.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;

import pe.edu.utp.medifast.dao.CitaDao;
import pe.edu.utp.medifast.dao.CitaHorarioDao;
import pe.edu.utp.medifast.dao.HorarioDao;
import pe.edu.utp.medifast.dao.MedicoDao;
import pe.edu.utp.medifast.dao.PacienteDao;
import pe.edu.utp.medifast.dao.TarifaDao;
import pe.edu.utp.medifast.dao.VistaCitaDao;
import pe.edu.utp.medifast.dto.DisponibilidadDto;
import pe.edu.utp.medifast.exception.CitaActivaException;
import pe.edu.utp.medifast.exception.FueraDeRangoException;
import pe.edu.utp.medifast.exception.HorarioNoDisponibleException;
import pe.edu.utp.medifast.model.Cita;
import pe.edu.utp.medifast.model.Consultorio;
import pe.edu.utp.medifast.model.Horario;
import pe.edu.utp.medifast.model.Medico;
import pe.edu.utp.medifast.model.Paciente;
import pe.edu.utp.medifast.model.Tarifa;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;
import pe.edu.utp.medifast.model.enums.TipoCitaEnum;
import pe.edu.utp.medifast.util.Auditoria;
import pe.edu.utp.medifast.util.Auditoria.Evento;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservaServiceImplTest {

	private static final long ID_PACIENTE = 7L;
	private static final long ID_MEDICO = 3L;
	private static final LocalDate HOY = LocalDate.of(2026, 10, 26);
	private static final LocalDate FECHA_RESERVA = HOY.plusDays(1);
	private static final Instant AHORA = Instant.parse("2026-10-26T15:00:00Z");
	private static final Clock RELOJ_FIJO = Clock.fixed(AHORA, ZoneId.of("America/Lima"));

	@Mock
	private PacienteDao pacienteDao;
	@Mock
	private MedicoDao medicoDao;
	@Mock
	private CitaDao citaDao;
	@Mock
	private CitaHorarioDao citaHorarioDao;
	@Mock
	private HorarioDao horarioDao;
	@Mock
	private TarifaDao tarifaDao;
	@Mock
	private VistaCitaDao vistaCitaDao;
	@Mock
	private Auditoria auditoria;
	@Mock
	private Paciente paciente;
	@Mock
	private Medico medico;
	@Mock
	private Consultorio consultorio;
	@Mock
	private Tarifa tarifaNuevo;
	@Mock
	private Tarifa tarifaControl;
	@Mock
	private pe.edu.utp.medifast.model.VistaCita vistaCita;

	private ReservaServiceImpl servicio;

	@BeforeEach
	void configurarServicio() {
		servicio = new ReservaServiceImpl(pacienteDao, medicoDao, citaDao, citaHorarioDao, horarioDao, tarifaDao,
				vistaCitaDao, auditoria, RELOJ_FIJO);
		when(pacienteDao.findById(ID_PACIENTE)).thenReturn(Optional.of(paciente));
		when(medicoDao.findById(ID_MEDICO)).thenReturn(Optional.of(medico));
		when(medico.getConsultorio()).thenReturn(consultorio);
		when(consultorio.getDiasAnticipacionMin()).thenReturn(1);
		when(consultorio.getDiasAnticipacionMax()).thenReturn(30);
		when(consultorio.getMinutosBloque()).thenReturn(30);
		when(consultorio.getMinutosApartado()).thenReturn(15);
		when(tarifaDao.findByMedicoIdAndTipoCita(ID_MEDICO, TipoCitaEnum.NUEVO))
				.thenReturn(Optional.of(tarifaNuevo));
		when(tarifaNuevo.getDuracionMin()).thenReturn(60);
		when(tarifaNuevo.getPrecio()).thenReturn(new BigDecimal("150.00"));
		when(tarifaDao.findByMedicoIdAndTipoCita(ID_MEDICO, TipoCitaEnum.CONTROL))
				.thenReturn(Optional.of(tarifaControl));
		when(tarifaControl.getDuracionMin()).thenReturn(30);
		when(tarifaControl.getPrecio()).thenReturn(new BigDecimal("80.00"));
	}

	@Test
	void disponibilidadDetectaNuevoYOfreceSoloIniciosConDosBloquesConsecutivos() {
		when(citaDao.existsByPacienteIdAndMedicoIdAndEstado(ID_PACIENTE, ID_MEDICO, EstadoCitaEnum.ATENDIDA))
				.thenReturn(false);
		when(citaDao.findFirstByPacienteIdAndEstadoIn(any(), any())).thenReturn(Optional.empty());
		List<Horario> bloques = List.of(horario(1L, "09:00", "09:30"),
				horario(2L, "10:00", "10:30"), horario(3L, "10:30", "11:00"));
		when(horarioDao.bloquesDelDia(ID_MEDICO, FECHA_RESERVA)).thenReturn(bloques);
		when(citaHorarioDao.findIdsActivosByMedicoAndFecha(ID_MEDICO, FECHA_RESERVA)).thenReturn(List.of());

		DisponibilidadDto disponibilidad = servicio.disponibilidad(ID_PACIENTE, ID_MEDICO, FECHA_RESERVA);

		assertThat(disponibilidad.tipo()).isEqualTo(TipoCitaEnum.NUEVO);
		assertThat(disponibilidad.duracionMin()).isEqualTo(60);
		assertThat(disponibilidad.precio()).isEqualByComparingTo("150.00");
		assertThat(disponibilidad.inicios()).extracting("horaInicio")
				.containsExactly(LocalTime.of(10, 0));
		assertThat(disponibilidad.inicios().getFirst().horaFin()).isEqualTo(LocalTime.of(11, 0));
	}

	@Test
	void disponibilidadUsaTarifaControlDespuesDeUnaCitaAtendida() {
		when(citaDao.existsByPacienteIdAndMedicoIdAndEstado(ID_PACIENTE, ID_MEDICO, EstadoCitaEnum.ATENDIDA))
				.thenReturn(true);
		when(citaDao.findFirstByPacienteIdAndEstadoIn(any(), any())).thenReturn(Optional.empty());
		List<Horario> bloques = List.of(horario(1L, "09:00", "09:30"));
		when(horarioDao.bloquesDelDia(ID_MEDICO, FECHA_RESERVA)).thenReturn(bloques);
		when(citaHorarioDao.findIdsActivosByMedicoAndFecha(ID_MEDICO, FECHA_RESERVA)).thenReturn(List.of());

		DisponibilidadDto disponibilidad = servicio.disponibilidad(ID_PACIENTE, ID_MEDICO, FECHA_RESERVA);

		assertThat(disponibilidad.tipo()).isEqualTo(TipoCitaEnum.CONTROL);
		assertThat(disponibilidad.duracionMin()).isEqualTo(30);
		assertThat(disponibilidad.precio()).isEqualByComparingTo("80.00");
		assertThat(disponibilidad.inicios()).hasSize(1);
	}

	@Test
	void disponibilidadExponeBloquesOcupadosSinOfrecerlosComoInicios() {
		when(citaDao.existsByPacienteIdAndMedicoIdAndEstado(ID_PACIENTE, ID_MEDICO, EstadoCitaEnum.ATENDIDA))
				.thenReturn(false);
		when(citaDao.findFirstByPacienteIdAndEstadoIn(any(), any())).thenReturn(Optional.empty());
		List<Horario> bloques = List.of(horario(1L, "09:00", "09:30"), horario(2L, "09:30", "10:00"),
				horario(3L, "10:00", "10:30"));
		when(horarioDao.bloquesDelDia(ID_MEDICO, FECHA_RESERVA)).thenReturn(bloques);
		when(citaHorarioDao.findIdsActivosByMedicoAndFecha(ID_MEDICO, FECHA_RESERVA)).thenReturn(List.of(2L));

		DisponibilidadDto disponibilidad = servicio.disponibilidad(ID_PACIENTE, ID_MEDICO, FECHA_RESERVA);

		assertThat(disponibilidad.bloques()).extracting("ocupado").containsExactly(false, true, false);
		assertThat(disponibilidad.inicios()).isEmpty();
	}

	@Test
	void disponibilidadRechazaFechasFueraDeLaVentana() {
		assertThatThrownBy(() -> servicio.disponibilidad(ID_PACIENTE, ID_MEDICO, HOY))
				.isInstanceOf(FueraDeRangoException.class);
		assertThatThrownBy(() -> servicio.disponibilidad(ID_PACIENTE, ID_MEDICO, HOY.plusDays(31)))
				.isInstanceOf(FueraDeRangoException.class);
	}

	@Test
	void disponibilidadIncluyeCitaActivaDelPaciente() {
		Cita citaActiva = org.mockito.Mockito.mock(Cita.class);
		when(citaActiva.getId()).thenReturn(55L);
		when(citaDao.findFirstByPacienteIdAndEstadoIn(any(), any())).thenReturn(Optional.of(citaActiva));
		when(vistaCitaDao.findById(55L)).thenReturn(Optional.of(vistaCita));
		when(citaDao.existsByPacienteIdAndMedicoIdAndEstado(ID_PACIENTE, ID_MEDICO, EstadoCitaEnum.ATENDIDA))
				.thenReturn(false);
		when(horarioDao.bloquesLibres(ID_MEDICO, FECHA_RESERVA)).thenReturn(List.of());

		DisponibilidadDto disponibilidad = servicio.disponibilidad(ID_PACIENTE, ID_MEDICO, FECHA_RESERVA);

		assertThat(disponibilidad.citaActiva()).contains(vistaCita);
	}

	@Test
	void reservarCreaCitaPendienteConExpiracionYDosBloques() {
		when(citaDao.findFirstByPacienteIdAndEstadoIn(any(), any())).thenReturn(Optional.empty());
		Horario inicio = horario(1L, "10:00", "10:30");
		Horario siguiente = horario(2L, "10:30", "11:00");
		when(horarioDao.findById(1L)).thenReturn(Optional.of(inicio));
		when(horarioDao.bloquesLibres(ID_MEDICO, FECHA_RESERVA)).thenReturn(List.of(inicio, siguiente));
		when(citaDao.existsByPacienteIdAndMedicoIdAndEstado(ID_PACIENTE, ID_MEDICO, EstadoCitaEnum.ATENDIDA))
				.thenReturn(false);
		when(citaDao.saveAndFlush(any(Cita.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

		Cita cita = servicio.reservar(ID_PACIENTE, 1L);

		assertThat(cita.getEstado()).isEqualTo(EstadoCitaEnum.PENDIENTE_PAGO);
		assertThat(cita.getTipoCita()).isEqualTo(TipoCitaEnum.NUEVO);
		assertThat(cita.getExpiraEn()).isEqualTo(LocalDateTime.ofInstant(AHORA, RELOJ_FIJO.getZone()).plusMinutes(15));
		assertThat(cita.getBloques()).hasSize(2);
		verify(auditoria).registrar(org.mockito.ArgumentMatchers.eq(Evento.RESERVA), any(String.class));
	}

	@Test
	void reservarRechazaPacienteConCitaActiva() {
		Cita citaActiva = org.mockito.Mockito.mock(Cita.class);
		when(citaDao.findFirstByPacienteIdAndEstadoIn(any(), any())).thenReturn(Optional.of(citaActiva));

		assertThatThrownBy(() -> servicio.reservar(ID_PACIENTE, 1L))
				.isInstanceOf(CitaActivaException.class)
				.satisfies(excepcion -> assertThat(((CitaActivaException) excepcion).getCitaActiva())
						.isSameAs(citaActiva));
	}

	@Test
	void reservarTraduceConflictoDeHorarioDeLaBaseDeDatos() {
		when(citaDao.findFirstByPacienteIdAndEstadoIn(any(), any())).thenReturn(Optional.empty());
		Horario inicio = horario(1L, "10:00", "10:30");
		Horario siguiente = horario(2L, "10:30", "11:00");
		when(horarioDao.findById(1L)).thenReturn(Optional.of(inicio));
		when(horarioDao.bloquesLibres(ID_MEDICO, FECHA_RESERVA)).thenReturn(List.of(inicio, siguiente));
		when(citaDao.existsByPacienteIdAndMedicoIdAndEstado(ID_PACIENTE, ID_MEDICO, EstadoCitaEnum.ATENDIDA))
				.thenReturn(false);
		when(citaDao.saveAndFlush(any(Cita.class))).thenThrow(violacionUnica("uq_cita_horario_activo"));

		assertThatThrownBy(() -> servicio.reservar(ID_PACIENTE, 1L))
				.isInstanceOf(HorarioNoDisponibleException.class);
	}

	@Test
	void reservarTraduceConflictoDeCitaActivaDeLaBaseDeDatos() {
		Cita citaActiva = org.mockito.Mockito.mock(Cita.class);
		when(citaDao.findFirstByPacienteIdAndEstadoIn(any(), any()))
				.thenReturn(Optional.empty(), Optional.of(citaActiva));
		Horario inicio = horario(1L, "10:00", "10:30");
		Horario siguiente = horario(2L, "10:30", "11:00");
		when(horarioDao.findById(1L)).thenReturn(Optional.of(inicio));
		when(horarioDao.bloquesLibres(ID_MEDICO, FECHA_RESERVA)).thenReturn(List.of(inicio, siguiente));
		when(citaDao.existsByPacienteIdAndMedicoIdAndEstado(ID_PACIENTE, ID_MEDICO, EstadoCitaEnum.ATENDIDA))
				.thenReturn(false);
		when(citaDao.saveAndFlush(any(Cita.class))).thenThrow(violacionUnica("uq_cita_paciente_activo"));

		assertThatThrownBy(() -> servicio.reservar(ID_PACIENTE, 1L))
				.isInstanceOf(CitaActivaException.class)
				.satisfies(excepcion -> assertThat(((CitaActivaException) excepcion).getCitaActiva())
						.isSameAs(citaActiva));
	}

	private static Horario horario(long id, String inicio, String fin) {
		Horario horario = org.mockito.Mockito.mock(Horario.class);
		Medico medicoHorario = org.mockito.Mockito.mock(Medico.class);
		when(medicoHorario.getId()).thenReturn(ID_MEDICO);
		when(horario.getId()).thenReturn(id);
		when(horario.getMedico()).thenReturn(medicoHorario);
		when(horario.getFecha()).thenReturn(FECHA_RESERVA);
		when(horario.getHoraInicio()).thenReturn(LocalTime.parse(inicio));
		when(horario.getHoraFin()).thenReturn(LocalTime.parse(fin));
		when(horario.isBloqueado()).thenReturn(false);
		return horario;
	}

	private static DataIntegrityViolationException violacionUnica(String constraint) {
		return new DataIntegrityViolationException("duplicate", new SQLException("Duplicate key " + constraint));
	}
}
