package pe.edu.utp.medifast.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import pe.edu.utp.medifast.dao.ConsultorioDao;
import pe.edu.utp.medifast.model.Consultorio;
import pe.edu.utp.medifast.service.GeneracionHorarioService;

@Component
public class GeneracionHorarioJob {

	private static final Logger LOG = LoggerFactory.getLogger(GeneracionHorarioJob.class);
	private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

	private final GeneracionHorarioService generacionHorarioService;
	private final ConsultorioDao consultorioDao;
	private final Clock clock;

	public GeneracionHorarioJob(GeneracionHorarioService generacionHorarioService, ConsultorioDao consultorioDao,
			Clock clock) {
		this.generacionHorarioService = generacionHorarioService;
		this.consultorioDao = consultorioDao;
		this.clock = clock;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void alIniciar() {
		ejecutar();
	}

	@Scheduled(cron = "0 0 2 * * *", zone = "America/Lima")
	public void ejecutarDiariamente() {
		ejecutar();
	}

	void ejecutar() {
		LocalDate hoy = LocalDate.now(clock.withZone(ZONA_LIMA));
		int diasAnticipacionMax = consultorioDao.findAllByActivoTrue().stream()
				.mapToInt(Consultorio::getDiasAnticipacionMax)
				.max()
				.orElse(0);
		int generados = generacionHorarioService.generar(hoy, hoy.plusDays(diasAnticipacionMax));
		LOG.info("Generación de horarios completada: {} bloques creados para el rango {} a {}",
				generados, hoy, hoy.plusDays(diasAnticipacionMax));
	}
}
