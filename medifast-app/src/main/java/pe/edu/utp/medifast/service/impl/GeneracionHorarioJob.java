package pe.edu.utp.medifast.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import pe.edu.utp.medifast.dao.ConsultorioDao;
import pe.edu.utp.medifast.model.Consultorio;
import pe.edu.utp.medifast.service.GeneracionHorarioService;

/** Genera hoy y los días futuros de reserva al iniciar y cada noche a las 02:00. */
@Component
public class GeneracionHorarioJob {

	private static final Logger LOG = LoggerFactory.getLogger(GeneracionHorarioJob.class);
	private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

	private final GeneracionHorarioService generacionHorarioService;
	private final ConsultorioDao consultorioDao;
	private final Clock reloj;

	public GeneracionHorarioJob(GeneracionHorarioService generacionHorarioService, ConsultorioDao consultorioDao,
			Clock reloj) {
		this.generacionHorarioService = generacionHorarioService;
		this.consultorioDao = consultorioDao;
		this.reloj = reloj;
	}

	@EventListener(ApplicationReadyEvent.class)
	@Scheduled(cron = "0 0 2 * * *", zone = "America/Lima")
	public void ejecutar() {
		List<Consultorio> consultoriosActivos = consultorioDao.findAllByActivoTrue();
		if (consultoriosActivos.isEmpty()) {
			LOG.warn("No se generaron bloques: no hay consultorios activos.");
			return;
		}

		int diasMaximos = consultoriosActivos.stream()
				.mapToInt(Consultorio::getDiasAnticipacionMax)
				.max()
				.orElseThrow();
		LocalDate hoy = LocalDate.now(reloj.withZone(ZONA_LIMA));
		int creados = generacionHorarioService.generar(hoy, hoy.plusDays(diasMaximos));
		LOG.info("Generación de horarios completada: {} bloques creados entre {} y {}.",
				creados, hoy, hoy.plusDays(diasMaximos));
	}
}
