package pe.edu.utp.medifast.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import pe.edu.utp.medifast.dao.HorarioSemanalDao;
import pe.edu.utp.medifast.dao.VistaCitaDao;
import pe.edu.utp.medifast.dto.AgendaDto;
import pe.edu.utp.medifast.dto.FiltroAgendaDto;
import pe.edu.utp.medifast.dto.FiltroAgendaDto.Vista;
import pe.edu.utp.medifast.model.HorarioSemanal;
import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;
import pe.edu.utp.medifast.model.enums.EstadoPagoEnum;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GestionCitaServiceImplTest {

	private static final long ID_MEDICO = 1L;
	/** Miércoles: la semana va del lunes 9 al domingo 15 de noviembre. */
	private static final LocalDate MIERCOLES = LocalDate.of(2026, 11, 11);

	@Mock
	private VistaCitaDao vistaCitaDao;
	@Mock
	private HorarioSemanalDao horarioSemanalDao;

	@InjectMocks
	private GestionCitaServiceImpl gestionCitaService;

	@Test
	void vistaDiaConsultaSoloEseDia() {
		when(vistaCitaDao.findAllByIdMedicoAndFechaBetweenOrderByFechaAscHoraInicioAsc(ID_MEDICO, MIERCOLES,
				MIERCOLES)).thenReturn(List.of());

		gestionCitaService.agenda(ID_MEDICO, new FiltroAgendaDto(Vista.DIA, MIERCOLES, null));

		verify(vistaCitaDao).findAllByIdMedicoAndFechaBetweenOrderByFechaAscHoraInicioAsc(ID_MEDICO, MIERCOLES,
				MIERCOLES);
	}

	@Test
	void vistaSemanaConsultaDeLunesADomingo() {
		LocalDate lunes = LocalDate.of(2026, 11, 9);
		LocalDate domingo = LocalDate.of(2026, 11, 15);
		when(vistaCitaDao.findAllByIdMedicoAndFechaBetweenOrderByFechaAscHoraInicioAsc(ID_MEDICO, lunes, domingo))
				.thenReturn(List.of());

		gestionCitaService.agenda(ID_MEDICO, new FiltroAgendaDto(Vista.SEMANA, MIERCOLES, null));

		verify(vistaCitaDao).findAllByIdMedicoAndFechaBetweenOrderByFechaAscHoraInicioAsc(ID_MEDICO, lunes, domingo);
	}

	@Test
	void indicadoresCuentanTodasLasCitasDelPeriodo() {
		List<VistaCita> citas = datosDeEjemplo();
		when(vistaCitaDao.findAllByIdMedicoAndFechaBetweenOrderByFechaAscHoraInicioAsc(ID_MEDICO, MIERCOLES,
				MIERCOLES)).thenReturn(citas);

		AgendaDto agenda = gestionCitaService.agenda(ID_MEDICO, new FiltroAgendaDto(Vista.DIA, MIERCOLES, null));

		assertThat(agenda.citas()).hasSize(6);
		assertThat(agenda.total()).isEqualTo(6);
		assertThat(agenda.porValidar()).isEqualTo(1);
		assertThat(agenda.confirmadas()).isEqualTo(2);
		assertThat(agenda.atendidas()).isEqualTo(1);
	}

	@Test
	void filtroPorEstadoReduceLaListaPeroNoLosIndicadores() {
		List<VistaCita> citas = datosDeEjemplo();
		when(vistaCitaDao.findAllByIdMedicoAndFechaBetweenOrderByFechaAscHoraInicioAsc(ID_MEDICO, MIERCOLES,
				MIERCOLES)).thenReturn(citas);

		AgendaDto agenda = gestionCitaService.agenda(ID_MEDICO,
				new FiltroAgendaDto(Vista.DIA, MIERCOLES, EstadoCitaEnum.PROGRAMADA));

		assertThat(agenda.citas()).extracting(VistaCita::getEstado).containsOnly(EstadoCitaEnum.PROGRAMADA);
		assertThat(agenda.citas()).extracting(VistaCita::getIdCita).containsExactly(3L, 4L);
		assertThat(agenda.total()).isEqualTo(6);
		assertThat(agenda.confirmadas()).isEqualTo(2);
	}

	@Test
	void porValidarIgnoraPagosDeCitasCanceladas() {
		List<VistaCita> citas = List.of(
				cita(1L, EstadoCitaEnum.PROGRAMADA, EstadoPagoEnum.POR_VALIDAR),
				cita(2L, EstadoCitaEnum.CANCELADA, EstadoPagoEnum.POR_VALIDAR));
		when(vistaCitaDao.findAllByIdMedicoAndFechaBetweenOrderByFechaAscHoraInicioAsc(ID_MEDICO, MIERCOLES,
				MIERCOLES)).thenReturn(citas);

		AgendaDto agenda = gestionCitaService.agenda(ID_MEDICO, new FiltroAgendaDto(Vista.DIA, MIERCOLES, null));

		assertThat(agenda.porValidar()).isEqualTo(1);
	}

	@Test
	void devuelveElHorarioSemanalDelMedico() {
		HorarioSemanal horario = mock(HorarioSemanal.class);
		when(vistaCitaDao.findAllByIdMedicoAndFechaBetweenOrderByFechaAscHoraInicioAsc(ID_MEDICO, MIERCOLES,
				MIERCOLES)).thenReturn(List.of());
		when(horarioSemanalDao.findAllByMedicoId(ID_MEDICO)).thenReturn(List.of(horario));

		AgendaDto agenda = gestionCitaService.agenda(ID_MEDICO, new FiltroAgendaDto(Vista.DIA, MIERCOLES, null));

		assertThat(agenda.horario()).containsExactly(horario);
	}

	@Test
	void exigeMedicoYFiltro() {
		FiltroAgendaDto filtro = new FiltroAgendaDto(Vista.DIA, MIERCOLES, null);

		assertThatThrownBy(() -> gestionCitaService.agenda(null, filtro)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> gestionCitaService.agenda(ID_MEDICO, null))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	void filtroExigeVistaYFecha() {
		assertThatThrownBy(() -> new FiltroAgendaDto(null, MIERCOLES, null))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new FiltroAgendaDto(Vista.DIA, null, null))
				.isInstanceOf(NullPointerException.class);
	}

	/** Las 6 citas del miércoles 11 de noviembre del diseño aprobado (P7 v2). */
	private static List<VistaCita> datosDeEjemplo() {
		return List.of(
				cita(1L, EstadoCitaEnum.ATENDIDA, EstadoPagoEnum.VALIDADO),
				cita(2L, EstadoCitaEnum.NO_ASISTIO, EstadoPagoEnum.VALIDADO),
				cita(3L, EstadoCitaEnum.PROGRAMADA, EstadoPagoEnum.POR_VALIDAR),
				cita(4L, EstadoCitaEnum.PROGRAMADA, EstadoPagoEnum.VALIDADO),
				cita(5L, EstadoCitaEnum.PENDIENTE_PAGO, null),
				cita(6L, EstadoCitaEnum.CANCELADA, EstadoPagoEnum.RECHAZADO));
	}

	private static VistaCita cita(long id, EstadoCitaEnum estado, EstadoPagoEnum estadoPago) {
		VistaCita cita = mock(VistaCita.class);
		when(cita.getIdCita()).thenReturn(id);
		when(cita.getEstado()).thenReturn(estado);
		when(cita.getEstadoPago()).thenReturn(estadoPago);
		return cita;
	}
}
