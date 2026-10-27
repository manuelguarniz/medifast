package pe.edu.utp.medifast.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.function.Predicate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.utp.medifast.dao.HorarioSemanalDao;
import pe.edu.utp.medifast.dao.VistaCitaDao;
import pe.edu.utp.medifast.dto.AgendaDto;
import pe.edu.utp.medifast.dto.FiltroAgendaDto;
import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;
import pe.edu.utp.medifast.model.enums.EstadoPagoEnum;
import pe.edu.utp.medifast.service.GestionCitaService;

/** Agenda del médico sobre la vista {@code v_cita} con una sola consulta por período (RNF08). */
@Service
public class GestionCitaServiceImpl implements GestionCitaService {

	private final VistaCitaDao vistaCitaDao;
	private final HorarioSemanalDao horarioSemanalDao;

	public GestionCitaServiceImpl(VistaCitaDao vistaCitaDao, HorarioSemanalDao horarioSemanalDao) {
		this.vistaCitaDao = vistaCitaDao;
		this.horarioSemanalDao = horarioSemanalDao;
	}

	@Override
	@Transactional(readOnly = true)
	public AgendaDto agenda(Long idMedico, FiltroAgendaDto filtro) {
		checkNotNull(idMedico, "El médico es obligatorio");
		checkNotNull(filtro, "El filtro es obligatorio");
		List<VistaCita> periodo = vistaCitaDao.findAllByIdMedicoAndFechaBetweenOrderByFechaAscHoraInicioAsc(
				idMedico, filtro.desde(), filtro.hasta());
		List<VistaCita> visibles = filtro.estado() == null
				? periodo
				: periodo.stream().filter(cita -> cita.getEstado() == filtro.estado()).toList();
		// "Por validar" cuenta solo los pagos que el médico aún puede validar o rechazar (RF13).
		return new AgendaDto(
				visibles,
				periodo.size(),
				contar(periodo, cita -> cita.getEstado() == EstadoCitaEnum.PROGRAMADA
						&& cita.getEstadoPago() == EstadoPagoEnum.POR_VALIDAR),
				contar(periodo, cita -> cita.getEstado() == EstadoCitaEnum.PROGRAMADA),
				contar(periodo, cita -> cita.getEstado() == EstadoCitaEnum.ATENDIDA),
				horarioSemanalDao.findAllByMedicoId(idMedico));
	}

	private static long contar(List<VistaCita> citas, Predicate<VistaCita> condicion) {
		return citas.stream().filter(condicion).count();
	}
}
