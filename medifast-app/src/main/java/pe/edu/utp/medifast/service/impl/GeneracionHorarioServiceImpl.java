package pe.edu.utp.medifast.service.impl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.utp.medifast.dao.HorarioDao;
import pe.edu.utp.medifast.dao.HorarioSemanalDao;
import pe.edu.utp.medifast.dao.MedicoDao;
import pe.edu.utp.medifast.model.Horario;
import pe.edu.utp.medifast.model.HorarioSemanal;
import pe.edu.utp.medifast.model.Medico;
import pe.edu.utp.medifast.service.GeneracionHorarioService;

/** Construye los bloques diarios de atención y evita crear horarios ya existentes. */
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
		Objects.requireNonNull(desde, "La fecha inicial es obligatoria.");
		Objects.requireNonNull(hasta, "La fecha final es obligatoria.");
		if (desde.isAfter(hasta)) {
			throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final.");
		}

		int creados = 0;
		for (Medico medico : medicoDao.findAllByActivoTrue()) {
			int minutosBloque = medico.getConsultorio().getMinutosBloque();
			if (minutosBloque <= 0) {
				throw new IllegalStateException("El consultorio debe tener una duración de bloque positiva.");
			}

			List<HorarioSemanal> plantillas = horarioSemanalDao.findAllByMedicoId(medico.getId());
			if (plantillas.isEmpty()) {
				continue;
			}

			Set<BloqueClave> existentes = new HashSet<>();
			horarioDao.findAllByMedicoIdAndFechaBetween(medico.getId(), desde, hasta)
					.forEach(horario -> existentes.add(new BloqueClave(horario.getFecha(), horario.getHoraInicio())));
			Set<BloqueClave> nuevosClaves = new HashSet<>();
			List<Horario> nuevos = new ArrayList<>();

			for (LocalDate fecha = desde; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
				for (HorarioSemanal plantilla : plantillas) {
					if (plantilla.getDiaSemana().getValue() != fecha.getDayOfWeek().getValue()) {
						continue;
					}
					generarBloques(medico, plantilla, fecha, minutosBloque, existentes, nuevosClaves, nuevos);
				}
			}

			if (!nuevos.isEmpty()) {
				horarioDao.saveAll(nuevos);
				creados += nuevos.size();
			}
		}
		return creados;
	}

	private static void generarBloques(Medico medico, HorarioSemanal plantilla, LocalDate fecha, int minutosBloque,
			Set<BloqueClave> existentes, Set<BloqueClave> nuevosClaves, List<Horario> nuevos) {
		LocalTime horaFinPlantilla = plantilla.getHoraFin();
		for (LocalTime inicio = plantilla.getHoraInicio();
				!inicio.plusMinutes(minutosBloque).isAfter(horaFinPlantilla);
				inicio = inicio.plusMinutes(minutosBloque)) {
			BloqueClave clave = new BloqueClave(fecha, inicio);
			if (existentes.contains(clave) || !nuevosClaves.add(clave)) {
				continue;
			}
			nuevos.add(new Horario(medico, fecha, inicio, inicio.plusMinutes(minutosBloque)));
		}
	}

	private record BloqueClave(LocalDate fecha, LocalTime horaInicio) {
	}
}
