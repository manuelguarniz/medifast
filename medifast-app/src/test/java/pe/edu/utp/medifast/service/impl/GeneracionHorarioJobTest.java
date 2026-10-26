package pe.edu.utp.medifast.service.impl;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.edu.utp.medifast.dao.ConsultorioDao;
import pe.edu.utp.medifast.model.Consultorio;
import pe.edu.utp.medifast.service.GeneracionHorarioService;

@ExtendWith(MockitoExtension.class)
class GeneracionHorarioJobTest {

	@Mock
	private GeneracionHorarioService generacionHorarioService;

	@Mock
	private ConsultorioDao consultorioDao;

	private GeneracionHorarioJob job;

	@BeforeEach
	void configurarJob() {
		Clock clock = Clock.fixed(Instant.parse("2026-10-25T15:00:00Z"), ZoneOffset.UTC);
		job = new GeneracionHorarioJob(generacionHorarioService, consultorioDao, clock);
	}

	@Test
	void alIniciarGeneraDesdeHoyHastaElMaximoDeAnticipacionActivo() {
		Consultorio consultorio30 = consultorioConAnticipacionMaxima(30);
		Consultorio consultorio14 = consultorioConAnticipacionMaxima(14);
		when(consultorioDao.findAllByActivoTrue()).thenReturn(List.of(consultorio30, consultorio14));

		job.alIniciar();

		verify(generacionHorarioService).generar(LocalDate.of(2026, 10, 25), LocalDate.of(2026, 11, 24));
	}

	@Test
	void ejecucionDiariaGeneraHastaLaAnticipacionMaximaConfigurada() {
		when(consultorioDao.findAllByActivoTrue()).thenReturn(List.of(consultorioConAnticipacionMaxima(30)));

		job.ejecutarDiariamente();

		verify(generacionHorarioService).generar(LocalDate.of(2026, 10, 25), LocalDate.of(2026, 11, 24));
	}

	private Consultorio consultorioConAnticipacionMaxima(int dias) {
		Consultorio consultorio = new Consultorio("Consultorio", "Dirección");
		consultorio.setDiasAnticipacionMax(dias);
		return consultorio;
	}
}
