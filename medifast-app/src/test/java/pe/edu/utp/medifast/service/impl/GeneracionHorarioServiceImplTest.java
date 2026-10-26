package pe.edu.utp.medifast.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import pe.edu.utp.medifast.dao.HorarioDao;
import pe.edu.utp.medifast.dao.HorarioSemanalDao;
import pe.edu.utp.medifast.dao.MedicoDao;
import pe.edu.utp.medifast.model.Consultorio;
import pe.edu.utp.medifast.model.Horario;
import pe.edu.utp.medifast.model.HorarioSemanal;
import pe.edu.utp.medifast.model.Medico;
import pe.edu.utp.medifast.model.Usuario;
import pe.edu.utp.medifast.model.enums.RolEnum;
import pe.edu.utp.medifast.model.enums.TipoDocumentoEnum;

@ExtendWith(MockitoExtension.class)
class GeneracionHorarioServiceImplTest {

	private static final LocalDate LUNES = LocalDate.of(2026, 10, 26);

	@Mock
	private MedicoDao medicoDao;

	@Mock
	private HorarioSemanalDao horarioSemanalDao;

	@Mock
	private HorarioDao horarioDao;

	private GeneracionHorarioServiceImpl service;
	private Medico medico;
	private HorarioSemanal lunes;

	@BeforeEach
	void configurarServicio() {
		service = new GeneracionHorarioServiceImpl(medicoDao, horarioSemanalDao, horarioDao);
		Consultorio consultorio = new Consultorio("Medifast", "Trujillo");
		Usuario usuario = new Usuario(TipoDocumentoEnum.DNI, "10000001", "hash", RolEnum.MEDICO);
		medico = new Medico(usuario, consultorio, "Moisés", "Barrantes", "000001", "Medicina interna");
		ReflectionTestUtils.setField(medico, "id", 1L);
		lunes = new HorarioSemanal(medico, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0));
	}

	@Test
	void generaOchoBloquesParaUnDiaDe9a13() {
		prepararLunesSinBloques();
		AtomicReference<List<Horario>> persistidos = new AtomicReference<>(List.of());
		when(horarioDao.saveAll(any())).thenAnswer(invocacion -> {
			List<Horario> guardados = convertirHorarios((Iterable<?>) invocacion.getArgument(0));
			persistidos.set(guardados);
			return guardados;
		});

		int creados = service.generar(LUNES, LUNES);

		assertThat(creados).isEqualTo(8);
		List<Horario> bloques = persistidos.get();
		assertThat(bloques).hasSize(8);
		assertThat(bloques.get(0).getHoraInicio()).isEqualTo(LocalTime.of(9, 0));
		assertThat(bloques.get(0).getHoraFin()).isEqualTo(LocalTime.of(9, 30));
		assertThat(bloques.get(7).getHoraInicio()).isEqualTo(LocalTime.of(12, 30));
		assertThat(bloques.get(7).getHoraFin()).isEqualTo(LocalTime.of(13, 0));
	}

	@Test
	void noGeneraBloquesEnDomingoSinPlantilla() {
		when(medicoDao.findAllByActivoTrue()).thenReturn(List.of(medico));
		when(horarioSemanalDao.findAllByMedicoId(1L)).thenReturn(List.of());

		assertThat(service.generar(LUNES.plusDays(6), LUNES.plusDays(6))).isZero();

		verify(horarioDao, never()).saveAll(any());
	}

	@Test
	void segundaEjecucionNoDuplicaBloquesExistentes() {
		when(medicoDao.findAllByActivoTrue()).thenReturn(List.of(medico));
		when(horarioSemanalDao.findAllByMedicoId(1L)).thenReturn(List.of(lunes));
		AtomicReference<List<Horario>> persistidos = new AtomicReference<>(List.of());
		when(horarioDao.findAllByMedicoIdAndFechaBetween(1L, LUNES, LUNES))
				.thenAnswer(invocation -> persistidos.get());
		when(horarioDao.saveAll(any())).thenAnswer(invocation -> {
			List<Horario> guardados = new ArrayList<>();
			((Iterable<?>) invocation.getArgument(0)).forEach(horario -> guardados.add((Horario) horario));
			persistidos.set(List.copyOf(guardados));
			return guardados;
		});

		assertThat(service.generar(LUNES, LUNES)).isEqualTo(8);
		assertThat(service.generar(LUNES, LUNES)).isZero();

		verify(horarioDao).saveAll(any());
	}

	@Test
	void horarioQueNoCalzaConElBloqueNoGeneraBloqueIncompleto() {
		prepararLunesSinBloques();
		lunes.setHoraFin(LocalTime.of(12, 45));
		assertThat(service.generar(LUNES, LUNES)).isEqualTo(7);
	}

	@Test
	void usaLaDuracionDeBloqueConfiguradaEnElConsultorio() {
		prepararLunesSinBloques();
		medico.getConsultorio().setMinutosBloque(20);
		lunes.setHoraFin(LocalTime.of(10, 0));

		assertThat(service.generar(LUNES, LUNES)).isEqualTo(3);
	}

	@Test
	void rechazaUnRangoInvertido() {
		assertThatThrownBy(() -> service.generar(LUNES.plusDays(1), LUNES))
				.isInstanceOf(IllegalArgumentException.class);
	}

	private void prepararLunesSinBloques() {
		when(medicoDao.findAllByActivoTrue()).thenReturn(List.of(medico));
		when(horarioSemanalDao.findAllByMedicoId(1L)).thenReturn(List.of(lunes));
		when(horarioDao.findAllByMedicoIdAndFechaBetween(1L, LUNES, LUNES)).thenReturn(List.of());
	}

	private static List<Horario> convertirHorarios(Iterable<?> horarios) {
		List<Horario> convertidos = new ArrayList<>();
		horarios.forEach(horario -> convertidos.add((Horario) horario));
		return convertidos;
	}
}
