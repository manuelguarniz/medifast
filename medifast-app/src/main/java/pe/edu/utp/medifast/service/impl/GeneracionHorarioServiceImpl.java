package pe.edu.utp.medifast.service.impl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.utp.medifast.dao.HorarioDao;
import pe.edu.utp.medifast.dao.HorarioSemanalDao;
import pe.edu.utp.medifast.dao.MedicoDao;
import pe.edu.utp.medifast.model.Horario;
import pe.edu.utp.medifast.model.HorarioSemanal;
import pe.edu.utp.medifast.model.Medico;
import pe.edu.utp.medifast.service.GeneracionHorarioService;

@Service
public class GeneracionHorarioServiceImpl implements GeneracionHorarioService {

	private final MedicoDao medicoDao;
	private final HorarioSemanalDao horarioSemanalDao;
	private final HorarioDao horarioDao;

	public GeneracionHorarioServiceImpl(MedicoDao medicoDao, HorarioSemanalDao horarioSemanalDao,
			HorarioDao horarioDao) {
		this.medicoDao = medicoDao;
		this.horarioSemanalDao = horarioSemanalDao;
		this.horarioDao = horarioDao;
	}

	@Override
	@Transactional
	public int generar(LocalDate desde, LocalDate hasta) {
		Objects.requireNonNull(desde, "La fecha inicial es obligatoria");
		Objects.requireNonNull(hasta, "La fecha final es obligatoria");
		if (desde.isAfter(hasta)) {
			throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final");
		}

		int generados = 0;
		for (Medico medico : medicoDao.findAllByActivoTrue()) {
			int minutosBloque = medico.getConsultorio().getMinutosBloque();
			if (minutosBloque <= 0) {
				throw new IllegalStateException("Los minutos por bloque deben ser mayores que cero");
			}

			List<HorarioSemanal> plantillas = horarioSemanalDao.findByMedicoId(medico.getId());
			for (LocalDate fecha = desde; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
				for (HorarioSemanal plantilla : plantillas) {
					if (plantilla.getDiaSemana() == fecha.getDayOfWeek()) {
						generados += generarBloques(medico, fecha, plantilla, minutosBloque);
					}
				}
			}
		}
		return generados;
	}

	private int generarBloques(Medico medico, LocalDate fecha, HorarioSemanal plantilla, int minutosBloque) {
		LocalTime hora = plantilla.getHoraInicio();
		LocalTime horaFin = plantilla.getHoraFin();
		int generados = 0;

		while (hora.toSecondOfDay() + minutosBloque * 60 <= horaFin.toSecondOfDay()) {
			LocalTime finBloque = hora.plusMinutes(minutosBloque);
			if (!horarioDao.existsByMedicoIdAndFechaAndHoraInicio(medico.getId(), fecha, hora)) {
				horarioDao.save(new Horario(medico, fecha, hora, finBloque));
				generados++;
			}
			hora = finBloque;
		}
		return generados;
	}
}
