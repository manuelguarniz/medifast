package pe.edu.utp.medifast.service.impl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.utp.medifast.dao.CitaDao;
import pe.edu.utp.medifast.dao.HorarioSemanalDao;
import pe.edu.utp.medifast.dao.PagoDao;
import pe.edu.utp.medifast.dao.TarifaDao;
import pe.edu.utp.medifast.dao.VistaCitaDao;
import pe.edu.utp.medifast.dto.AgendaDto;
import pe.edu.utp.medifast.dto.DetalleCitaDto;
import pe.edu.utp.medifast.dto.FiltroAgendaDto;
import pe.edu.utp.medifast.dto.MotivoDto;
import pe.edu.utp.medifast.exception.AccesoDenegadoException;
import pe.edu.utp.medifast.exception.AccionNoPermitidaException;
import pe.edu.utp.medifast.exception.PagoNoValidadoException;
import pe.edu.utp.medifast.exception.RecursoNoEncontradoException;
import pe.edu.utp.medifast.model.Cita;
import pe.edu.utp.medifast.model.Pago;
import pe.edu.utp.medifast.model.Tarifa;
import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.model.enums.CanceladaPorEnum;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;
import pe.edu.utp.medifast.model.enums.EstadoPagoEnum;
import pe.edu.utp.medifast.service.GestionCitaService;
import pe.edu.utp.medifast.util.Auditoria;
import pe.edu.utp.medifast.util.Auditoria.Evento;

/**
 * Agenda del médico sobre la vista {@code v_cita} con una sola consulta por período (RNF08) y acciones de P8 sobre
 * {@code cita}, {@code cita_horario} y {@code pago}, cada una en su transacción (RNF05).
 */
@Service
public class GestionCitaServiceImpl implements GestionCitaService {

	private static final String CITA_NO_ENCONTRADA = "No se encontró la cita.";
	private static final String SIN_ACCIONES = "La cita ya no admite esta acción. Actualiza la página.";

	private final VistaCitaDao vistaCitaDao;
	private final HorarioSemanalDao horarioSemanalDao;
	private final CitaDao citaDao;
	private final PagoDao pagoDao;
	private final TarifaDao tarifaDao;
	private final Auditoria auditoria;
	private final Clock clock;

	public GestionCitaServiceImpl(VistaCitaDao vistaCitaDao, HorarioSemanalDao horarioSemanalDao, CitaDao citaDao,
			PagoDao pagoDao, TarifaDao tarifaDao, Auditoria auditoria, Clock clock) {
		this.vistaCitaDao = vistaCitaDao;
		this.horarioSemanalDao = horarioSemanalDao;
		this.citaDao = citaDao;
		this.pagoDao = pagoDao;
		this.tarifaDao = tarifaDao;
		this.auditoria = auditoria;
		this.clock = clock;
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

	@Override
	@Transactional(readOnly = true)
	public DetalleCitaDto detalle(Long idCita, Long idMedico) {
		Cita cita = citaDelMedico(idCita, idMedico);
		VistaCita vista = vistaCitaDao.findById(idCita)
				.orElseThrow(() -> new RecursoNoEncontradoException(CITA_NO_ENCONTRADA));
		Pago pago = pagoDao.findByCitaId(idCita).orElse(null);
		BigDecimal monto = pago != null
				? pago.getMonto()
				: tarifaDao.findByMedicoIdAndTipoCita(idMedico, cita.getTipoCita()).map(Tarifa::getPrecio).orElse(null);
		return new DetalleCitaDto(vista, cita.getPaciente().getCelular(), cita.getPaciente().getCorreo(), monto, pago,
				iniciada(vista), cita.getActualizadoEn());
	}

	@Override
	@Transactional
	public void validarPago(Long idCita, Long idMedico) {
		Pago pago = pagoPorValidar(citaDelMedico(idCita, idMedico));
		pago.setEstado(EstadoPagoEnum.VALIDADO);
		pago.setValidadoEn(LocalDateTime.now(clock));
		auditoria.registrar(Evento.PAGO_VALIDADO, "id_cita=" + idCita);
	}

	@Override
	@Transactional
	public void rechazarPago(Long idCita, Long idMedico, String observacion) {
		String motivo = motivo(observacion);
		Cita cita = citaDelMedico(idCita, idMedico);
		Pago pago = pagoPorValidar(cita);
		pago.setEstado(EstadoPagoEnum.RECHAZADO);
		cancelarYLiberar(cita, motivo);
		auditoria.registrar(Evento.PAGO_RECHAZADO, "id_cita=" + idCita);
	}

	@Override
	@Transactional
	public void marcarAtendida(Long idCita, Long idMedico) {
		Cita cita = citaDelMedico(idCita, idMedico);
		verificarAsistencia(cita);
		boolean validado = pagoDao.findByCitaId(idCita)
				.map(pago -> pago.getEstado() == EstadoPagoEnum.VALIDADO)
				.orElse(false);
		if (!validado) {
			throw new PagoNoValidadoException();
		}
		cita.setEstado(EstadoCitaEnum.ATENDIDA);
		auditoria.registrar(Evento.ASISTENCIA, "id_cita=" + idCita + " estado=ATENDIDA");
	}

	@Override
	@Transactional
	public void marcarNoAsistio(Long idCita, Long idMedico) {
		Cita cita = citaDelMedico(idCita, idMedico);
		verificarAsistencia(cita);
		cita.setEstado(EstadoCitaEnum.NO_ASISTIO);
		auditoria.registrar(Evento.ASISTENCIA, "id_cita=" + idCita + " estado=NO_ASISTIO");
	}

	@Override
	@Transactional
	public void cancelar(Long idCita, Long idMedico, String observacion) {
		String motivo = motivo(observacion);
		Cita cita = citaDelMedico(idCita, idMedico);
		if (cita.getEstado() != EstadoCitaEnum.PENDIENTE_PAGO && cita.getEstado() != EstadoCitaEnum.PROGRAMADA) {
			throw new AccionNoPermitidaException(SIN_ACCIONES);
		}
		cancelarYLiberar(cita, motivo);
		auditoria.registrar(Evento.CANCELACION, "id_cita=" + idCita);
	}

	/** RF12: el médico solo opera sobre sus propias citas. */
	private Cita citaDelMedico(Long idCita, Long idMedico) {
		checkNotNull(idCita, "La cita es obligatoria");
		checkNotNull(idMedico, "El médico es obligatorio");
		Cita cita = citaDao.findById(idCita)
				.orElseThrow(() -> new RecursoNoEncontradoException(CITA_NO_ENCONTRADA));
		if (!cita.getMedico().getId().equals(idMedico)) {
			throw new AccesoDenegadoException("La cita no pertenece al médico autenticado.");
		}
		return cita;
	}

	/** RF13: solo se validan o rechazan pagos {@code POR_VALIDAR} de citas confirmadas. */
	private Pago pagoPorValidar(Cita cita) {
		return pagoDao.findByCitaId(cita.getId())
				.filter(pago -> pago.getEstado() == EstadoPagoEnum.POR_VALIDAR)
				.filter(pago -> cita.getEstado() == EstadoCitaEnum.PROGRAMADA)
				.orElseThrow(() -> new AccionNoPermitidaException(SIN_ACCIONES));
	}

	/** RF08 y RN-14: la cita está confirmada y su hora de inicio ya llegó. */
	private void verificarAsistencia(Cita cita) {
		if (cita.getEstado() != EstadoCitaEnum.PROGRAMADA) {
			throw new AccionNoPermitidaException(SIN_ACCIONES);
		}
		VistaCita vista = vistaCitaDao.findById(cita.getId())
				.orElseThrow(() -> new RecursoNoEncontradoException(CITA_NO_ENCONTRADA));
		if (!iniciada(vista)) {
			throw new AccionNoPermitidaException("La cita aún no empieza.");
		}
	}

	private boolean iniciada(VistaCita cita) {
		return !cita.getFecha().atTime(cita.getHoraInicio()).isAfter(LocalDateTime.now(clock));
	}

	/** RN-13 y la misma transacción de RF13/RF14: cancela con el motivo y libera los bloques (RN-01). */
	private static void cancelarYLiberar(Cita cita, String motivo) {
		cita.setEstado(EstadoCitaEnum.CANCELADA);
		cita.setObservacion(motivo);
		cita.setCanceladaPor(CanceladaPorEnum.MEDICO);
		cita.getBloques().forEach(bloque -> bloque.setActivo(false));
	}

	private static String motivo(String observacion) {
		String motivo = StringUtils.strip(observacion);
		checkArgument(StringUtils.isNotEmpty(motivo), "Ingresa el motivo.");
		checkArgument(motivo.length() <= MotivoDto.MAXIMO, "El motivo no puede superar los 500 caracteres.");
		return motivo;
	}

	private static long contar(List<VistaCita> citas, Predicate<VistaCita> condicion) {
		return citas.stream().filter(condicion).count();
	}
}
