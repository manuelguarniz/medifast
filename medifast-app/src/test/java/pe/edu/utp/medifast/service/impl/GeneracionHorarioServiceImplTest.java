package pe.edu.utp.medifast.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.edu.utp.medifast.dao.HorarioDao;
import pe.edu.utp.medifast.dao.HorarioSemanalDao;
import pe.edu.utp.medifast.dao.MedicoDao;
import pe.edu.utp.medifast.model.Consultorio;
import pe.edu.utp.medifast.model.Horario;
import pe.edu.utp.medifast.model.HorarioSemanal;
import pe.edu.utp.medifast.model.Medico;

@ExtendWith(MockitoExtension.class)
class GeneracionHorarioServiceImplTest {

	private static final LocalDate LUNES = LocalDate.of(2026, 10, 26);

	@Mock
	private MedicoDao medicoDao;

	@Mock
	private HorarioSemanalDao horarioSemanalDao;

	@Mock
	private HorarioDao horarioDao;

	@InjectMocks
	private GeneracionHorarioServiceImpl service;

	private Medico medico;
	private Consultorio consultorio;

	@BeforeEach
	void configurarMedicoActivo() {
		medico = mock(Medico.class);
		consultorio = mock(Consultorio.class);
		lenient().when(medicoDao.findAllByActivoTrue()).thenReturn(List.of(medico));
		lenient().when(medico.getId()).thenReturn(17L);
		lenient().when(medico.getConsultorio()).thenReturn(consultorio);
		lenient().when(consultorio.getMinutosBloque()).thenReturn(30);
	}

	@Test
	void generaOchoBloquesParaUnDiaDe9a13() {
		when(horarioSemanalDao.findByMedicoId(17L))
				.thenReturn(List.of(plantilla(DayOfWeek.MONDAY, "09:00", "13:00")));

		int generados = service.generar(LUNES, LUNES);

		assertThat(generados).isEqualTo(8);
		ArgumentCaptor<Horario> bloques = ArgumentCaptor.forClass(Horario.class);
		verify(horarioDao, org.mockito.Mockito.times(8)).save(bloques.capture());
		assertThat(bloques.getAllValues())
				.extracting(Horario::getHoraInicio)
				.containsExactly(
						LocalTime.of(9, 0), LocalTime.of(9, 30),
						LocalTime.of(10, 0), LocalTime.of(10, 30),
						LocalTime.of(11, 0), LocalTime.of(11, 30),
						LocalTime.of(12, 0), LocalTime.of(12, 30));
		assertThat(bloques.getAllValues()).allSatisfy(bloque -> {
			assertThat(bloque.getFecha()).isEqualTo(LUNES);
			assertThat(bloque.getHoraFin()).isEqualTo(bloque.getHoraInicio().plusMinutes(30));
			assertThat(bloque.getMedico()).isSameAs(medico);
		});
	}

	@Test
	void noGeneraBloquesEnDomingoSinPlantilla() {
		when(horarioSemanalDao.findByMedicoId(17L)).thenReturn(List.of());

		int generados = service.generar(LUNES.plusDays(6), LUNES.plusDays(6));

		assertThat(generados).isZero();
		verify(horarioDao, never()).save(org.mockito.ArgumentMatchers.any(Horario.class));
	}

	@Test
	void segundaEjecucionNoDuplicaBloquesExistentes() {
		when(horarioSemanalDao.findByMedicoId(17L))
				.thenReturn(List.of(plantilla(DayOfWeek.MONDAY, "09:00", "10:00")));
		when(horarioDao.existsByMedicoIdAndFechaAndHoraInicio(17L, LUNES, LocalTime.of(9, 0)))
				.thenReturn(true);
		when(horarioDao.existsByMedicoIdAndFechaAndHoraInicio(17L, LUNES, LocalTime.of(9, 30)))
				.thenReturn(true);

		int generados = service.generar(LUNES, LUNES);

		assertThat(generados).isZero();
		verify(horarioDao, never()).save(org.mockito.ArgumentMatchers.any(Horario.class));
	}

	@Test
	void usaDuracionConfiguradaEnConsultorioYNoGeneraBloqueIncompleto() {
		when(consultorio.getMinutosBloque()).thenReturn(60);
		when(horarioSemanalDao.findByMedicoId(17L))
				.thenReturn(List.of(plantilla(DayOfWeek.MONDAY, "09:00", "12:45")));

		int generados = service.generar(LUNES, LUNES);

		assertThat(generados).isEqualTo(3);
		ArgumentCaptor<Horario> bloques = ArgumentCaptor.forClass(Horario.class);
		verify(horarioDao, org.mockito.Mockito.times(3)).save(bloques.capture());
		assertThat(bloques.getAllValues())
				.extracting(Horario::getHoraFin)
				.containsExactly(LocalTime.of(10, 0), LocalTime.of(11, 0), LocalTime.of(12, 0));
	}

	@Test
	void horarioDe9a1245ConBloquesDe30GeneraSieteBloquesCompletos() {
		when(horarioSemanalDao.findByMedicoId(17L))
				.thenReturn(List.of(plantilla(DayOfWeek.MONDAY, "09:00", "12:45")));

		int generados = service.generar(LUNES, LUNES);

		assertThat(generados).isEqualTo(7);
	}

	@Test
	void soloGeneraLosDiasIncluidosEnLaPlantillaSemanal() {
		when(horarioSemanalDao.findByMedicoId(17L))
				.thenReturn(List.of(plantilla(DayOfWeek.TUESDAY, "09:00", "10:00")));

		int generados = service.generar(LUNES, LUNES.plusDays(1));

		assertThat(generados).isEqualTo(2);
		ArgumentCaptor<Horario> bloques = ArgumentCaptor.forClass(Horario.class);
		verify(horarioDao, org.mockito.Mockito.times(2)).save(bloques.capture());
		assertThat(bloques.getAllValues()).extracting(Horario::getFecha).containsOnly(LUNES.plusDays(1));
	}

	@Test
	void rechazaUnRangoDeFechasInvertido() {
		assertThatThrownBy(() -> service.generar(LUNES.plusDays(1), LUNES))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rechazaDuracionDeBloqueNoPositiva() {
		when(consultorio.getMinutosBloque()).thenReturn(0);

		assertThatThrownBy(() -> service.generar(LUNES, LUNES))
				.isInstanceOf(IllegalStateException.class);
	}

	private HorarioSemanal plantilla(DayOfWeek dia, String inicio, String fin) {
		return new HorarioSemanal(medico, dia, LocalTime.parse(inicio), LocalTime.parse(fin));
	}
}
