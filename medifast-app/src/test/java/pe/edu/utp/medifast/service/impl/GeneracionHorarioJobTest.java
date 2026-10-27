package pe.edu.utp.medifast.service.impl;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.edu.utp.medifast.dao.ConsultorioDao;
import pe.edu.utp.medifast.model.Consultorio;
import pe.edu.utp.medifast.service.GeneracionHorarioService;

@ExtendWith(MockitoExtension.class)
class GeneracionHorarioJobTest {

	private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");
	private static final LocalDate HOY = LocalDate.of(2026, 10, 25);

	@Mock
	private GeneracionHorarioService service;

	@Mock
	private ConsultorioDao consultorioDao;

	@Test
	void ejecutaGeneracionDesdeHoyHastaMaximoDeAnticipacion() {
		Consultorio consultorio = new Consultorio("Medifast", "Trujillo");
		consultorio.setDiasAnticipacionMax(30);
		when(consultorioDao.findAllByActivoTrue()).thenReturn(List.of(consultorio));
		GeneracionHorarioJob job = new GeneracionHorarioJob(service, consultorioDao,
				Clock.fixed(Instant.parse("2026-10-26T04:00:00Z"), ZONA_LIMA));

		job.ejecutar();

		verify(service).generar(HOY, HOY.plusDays(30));
	}

	@Test
	void noGeneraHorariosCuandoNoHayConsultoriosActivos() {
		when(consultorioDao.findAllByActivoTrue()).thenReturn(List.of());
		GeneracionHorarioJob job = new GeneracionHorarioJob(service, consultorioDao, Clock.system(ZONA_LIMA));

		job.ejecutar();

		verify(service, never()).generar(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
	}
}
