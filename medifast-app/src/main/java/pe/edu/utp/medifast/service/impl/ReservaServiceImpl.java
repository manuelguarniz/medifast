package pe.edu.utp.medifast.service.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.utp.medifast.dao.CitaDao;
import pe.edu.utp.medifast.dao.CitaHorarioDao;
import pe.edu.utp.medifast.dao.HorarioDao;
import pe.edu.utp.medifast.dao.MedicoDao;
import pe.edu.utp.medifast.dao.PacienteDao;
import pe.edu.utp.medifast.dao.TarifaDao;
import pe.edu.utp.medifast.dao.VistaCitaDao;
import pe.edu.utp.medifast.dto.DisponibilidadDto;
import pe.edu.utp.medifast.dto.BloqueHorarioDto;
import pe.edu.utp.medifast.dto.CitaPacienteDto;
import pe.edu.utp.medifast.dto.InicioDisponibleDto;
import pe.edu.utp.medifast.dto.MisCitasDto;
import pe.edu.utp.medifast.exception.AccesoDenegadoException;
import pe.edu.utp.medifast.exception.CitaActivaException;
import pe.edu.utp.medifast.exception.FueraDeRangoException;
import pe.edu.utp.medifast.exception.HorarioNoDisponibleException;
import pe.edu.utp.medifast.model.Cita;
import pe.edu.utp.medifast.model.Consultorio;
import pe.edu.utp.medifast.model.Horario;
import pe.edu.utp.medifast.model.Medico;
import pe.edu.utp.medifast.model.Paciente;
import pe.edu.utp.medifast.model.Tarifa;
import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;
import pe.edu.utp.medifast.model.enums.TipoCitaEnum;
import pe.edu.utp.medifast.service.ReservaService;
import pe.edu.utp.medifast.util.Auditoria;
import pe.edu.utp.medifast.util.Auditoria.Evento;
import pe.edu.utp.medifast.util.RestriccionesBd;

/** Implementación transaccional de disponibilidad y reserva (RN-01 a RN-09). */
@Service
public class ReservaServiceImpl implements ReservaService {

	private static final List<EstadoCitaEnum> ESTADOS_ACTIVOS =
			List.of(EstadoCitaEnum.PENDIENTE_PAGO, EstadoCitaEnum.PROGRAMADA);

	private final PacienteDao pacienteDao;
	private final MedicoDao medicoDao;
	private final CitaDao citaDao;
	private final CitaHorarioDao citaHorarioDao;
	private final HorarioDao horarioDao;
	private final TarifaDao tarifaDao;
	private final VistaCitaDao vistaCitaDao;
	private final Auditoria auditoria;
	private final Clock clock;

	public ReservaServiceImpl(PacienteDao pacienteDao, MedicoDao medicoDao, CitaDao citaDao, CitaHorarioDao citaHorarioDao,
			HorarioDao horarioDao, TarifaDao tarifaDao, VistaCitaDao vistaCitaDao, Auditoria auditoria, Clock clock) {
		this.pacienteDao = pacienteDao;
		this.medicoDao = medicoDao;
		this.citaDao = citaDao;
		this.citaHorarioDao = citaHorarioDao;
		this.horarioDao = horarioDao;
		this.tarifaDao = tarifaDao;
		this.vistaCitaDao = vistaCitaDao;
		this.auditoria = auditoria;
		this.clock = clock;
	}

	@Override
	@Transactional(readOnly = true)
	public DisponibilidadDto disponibilidad(Long idPaciente, Long idMedico, LocalDate fecha) {
		requirePaciente(idPaciente);
		Medico medico = requireMedico(idMedico);
		Consultorio consultorio = medico.getConsultorio();
		LocalDate fechaMin = fechaMin(consultorio);
		LocalDate fechaMax = fechaMax(consultorio);
		validarFecha(fecha, fechaMin, fechaMax);

		TipoCitaEnum tipo = detectarTipo(idPaciente, idMedico);
		Tarifa tarifa = requireTarifa(idMedico, tipo);
		int bloquesNecesarios = bloquesNecesarios(tarifa, consultorio.getMinutosBloque());
		List<Horario> bloquesDelDia = horarioDao.bloquesDelDia(idMedico, fecha);
		List<Long> horariosOcupados = citaHorarioDao.findIdsActivosByMedicoAndFecha(idMedico, fecha);
		List<Horario> bloquesLibres = bloquesDelDia.stream()
				.filter(bloque -> !bloque.isBloqueado() && !horariosOcupados.contains(bloque.getId()))
				.toList();
		List<InicioDisponibleDto> inicios = buscarInicios(bloquesLibres, bloquesNecesarios);
		List<BloqueHorarioDto> bloques = bloquesDelDia.stream()
				.map(bloque -> {
					boolean ocupado = horariosOcupados.contains(bloque.getId());
					return new BloqueHorarioDto(bloque.getId(), bloque.getHoraInicio(), bloque.getHoraFin(),
							!bloque.isBloqueado() && !ocupado, ocupado, bloque.isBloqueado());
				})
				.toList();
		Optional<VistaCita> citaActiva = citaDao.findFirstByPacienteIdAndEstadoIn(idPaciente, ESTADOS_ACTIVOS)
				.map(cita -> vistaCitaDao.findById(cita.getId()).orElseThrow(
						() -> new EntityNotFoundException("No se encontró el detalle de la cita activa.")));

		return new DisponibilidadDto(tipo, tarifa.getDuracionMin(), tarifa.getPrecio(), fechaMin, fechaMax, fecha,
				inicios, bloques, citaActiva);
	}

	@Override
	@Transactional(readOnly = true)
	public TipoCitaEnum detectarTipo(Long idPaciente, Long idMedico) {
		requirePaciente(idPaciente);
		requireMedico(idMedico);
		boolean tieneAtendida = citaDao.existsByPacienteIdAndMedicoIdAndEstado(idPaciente, idMedico,
				EstadoCitaEnum.ATENDIDA);
		return tieneAtendida ? TipoCitaEnum.CONTROL : TipoCitaEnum.NUEVO;
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public Cita reservar(Long idPaciente, Long idHorario) {
		Paciente paciente = requirePaciente(idPaciente);
		citaDao.findFirstByPacienteIdAndEstadoIn(idPaciente, ESTADOS_ACTIVOS)
				.ifPresent(cita -> {
					throw new CitaActivaException(cita);
				});

		Horario inicio = horarioDao.findById(idHorario)
				.orElseThrow(HorarioNoDisponibleException::new);
		Long idMedico = inicio.getMedico().getId();
		Medico medico = requireMedico(idMedico);
		Consultorio consultorio = medico.getConsultorio();
		LocalDate fecha = inicio.getFecha();
		validarFecha(fecha, fechaMin(consultorio), fechaMax(consultorio));
		if (inicio.isBloqueado()) {
			throw new HorarioNoDisponibleException();
		}

		TipoCitaEnum tipo = detectarTipo(idPaciente, idMedico);
		Tarifa tarifa = requireTarifa(idMedico, tipo);
		int cantidadBloques = bloquesNecesarios(tarifa, consultorio.getMinutosBloque());
		List<Horario> libres = horarioDao.bloquesLibres(idMedico, fecha);
		int indiceInicio = indiceBloque(libres, idHorario);
		if (indiceInicio < 0 || !inicio.getHoraInicio().equals(libres.get(indiceInicio).getHoraInicio())) {
			throw new HorarioNoDisponibleException();
		}
		List<Horario> seleccionados = seleccionarConsecutivos(libres, indiceInicio, cantidadBloques);
		if (seleccionados.isEmpty()) {
			throw new HorarioNoDisponibleException();
		}

		LocalDateTime expiraEn = LocalDateTime.now(clock).plusMinutes(consultorio.getMinutosApartado());
		Cita cita = new Cita(paciente, medico, tipo, expiraEn);
		seleccionados.forEach(cita::agregarBloque);
		try {
			Cita guardada = citaDao.saveAndFlush(cita);
			auditoria.registrar(Evento.RESERVA, "id_cita=" + guardada.getId());
			return guardada;
		} catch (DataIntegrityViolationException ex) {
			if (RestriccionesBd.viola(ex, "uq_cita_paciente_activo")) {
				Cita activa = citaDao.findFirstByPacienteIdAndEstadoIn(idPaciente, ESTADOS_ACTIVOS)
						.orElseThrow(() -> ex);
				throw new CitaActivaException(activa);
			}
			if (RestriccionesBd.viola(ex, "uq_cita_horario_activo")) {
				throw new HorarioNoDisponibleException();
			}
			throw ex;
		}
	}

	@Override
	@Transactional(readOnly = true)
	public MisCitasDto historial(Long idPaciente) {
		requirePaciente(idPaciente);
		LocalDateTime ahora = LocalDateTime.now(clock);
		Map<String, BigDecimal> preciosPorTarifa = new HashMap<>();
		CitaPacienteDto proxima = null;
		List<CitaPacienteDto> historial = new ArrayList<>();
		for (VistaCita cita : vistaCitaDao.findAllByIdPacienteOrderByFechaDescHoraInicioDesc(idPaciente)) {
			CitaPacienteDto dto = paraPaciente(cita, ahora, preciosPorTarifa);
			// RN-09 garantiza una sola activa; si la BD tuviera más, las demás quedan en el historial.
			boolean activa = cita.getEstado() == EstadoCitaEnum.PROGRAMADA || dto.pagoPendiente();
			if (activa && proxima == null) {
				proxima = dto;
			} else {
				historial.add(dto);
			}
		}
		return new MisCitasDto(Optional.ofNullable(proxima), List.copyOf(historial));
	}

	@Override
	@Transactional(readOnly = true)
	public CitaPacienteDto cita(Long idCita, Long idPaciente) {
		VistaCita cita = vistaCitaDao.findById(idCita)
				.filter(encontrada -> encontrada.getIdPaciente().equals(idPaciente))
				.orElseThrow(() -> new AccesoDenegadoException("La cita no pertenece al paciente autenticado."));
		return paraPaciente(cita, LocalDateTime.now(clock), new HashMap<>());
	}

	/** Monto (del pago o de la tarifa, RN-03), vencimiento y tiempo restante de una cita del paciente. */
	private CitaPacienteDto paraPaciente(VistaCita cita, LocalDateTime ahora, Map<String, BigDecimal> preciosPorTarifa) {
		boolean pendiente = cita.getEstado() == EstadoCitaEnum.PENDIENTE_PAGO;
		boolean vencida = pendiente && (cita.getExpiraEn() == null || !cita.getExpiraEn().isAfter(ahora));
		long segundosRestantes = pendiente && !vencida
				? Duration.between(ahora, cita.getExpiraEn()).toSeconds()
				: 0;
		BigDecimal monto = cita.getMonto() != null
				? cita.getMonto()
				: preciosPorTarifa.computeIfAbsent(cita.getIdMedico() + ":" + cita.getTipoCita(),
						clave -> requireTarifa(cita.getIdMedico(), cita.getTipoCita()).getPrecio());
		return new CitaPacienteDto(cita, monto, vencida, segundosRestantes);
	}

	private Paciente requirePaciente(Long idPaciente) {
		return pacienteDao.findById(idPaciente)
				.orElseThrow(() -> new EntityNotFoundException("No se encontró el paciente solicitado."));
	}

	private Medico requireMedico(Long idMedico) {
		return medicoDao.findById(idMedico)
				.orElseThrow(() -> new EntityNotFoundException("No se encontró el médico solicitado."));
	}

	private Tarifa requireTarifa(Long idMedico, TipoCitaEnum tipo) {
		return tarifaDao.findByMedicoIdAndTipoCita(idMedico, tipo)
				.orElseThrow(() -> new EntityNotFoundException("No se encontró la tarifa para el tipo de cita."));
	}

	private LocalDate fechaMin(Consultorio consultorio) {
		return LocalDate.now(clock).plusDays(consultorio.getDiasAnticipacionMin());
	}

	private LocalDate fechaMax(Consultorio consultorio) {
		return LocalDate.now(clock).plusDays(consultorio.getDiasAnticipacionMax());
	}

	private static void validarFecha(LocalDate fecha, LocalDate fechaMin, LocalDate fechaMax) {
		if (fecha == null || fecha.isBefore(fechaMin) || fecha.isAfter(fechaMax)) {
			throw new FueraDeRangoException();
		}
	}

	private static int bloquesNecesarios(Tarifa tarifa, int minutosBloque) {
		if (minutosBloque <= 0 || tarifa.getDuracionMin() <= 0) {
			throw new IllegalStateException("La duración de la tarifa y del bloque debe ser positiva.");
		}
		return (tarifa.getDuracionMin() + minutosBloque - 1) / minutosBloque;
	}

	private static List<InicioDisponibleDto> buscarInicios(List<Horario> libres, int cantidadBloques) {
		List<InicioDisponibleDto> inicios = new ArrayList<>();
		for (int indice = 0; indice + cantidadBloques <= libres.size(); indice++) {
			List<Horario> consecutivos = seleccionarConsecutivos(libres, indice, cantidadBloques);
			if (!consecutivos.isEmpty()) {
				Horario primero = consecutivos.getFirst();
				Horario ultimo = consecutivos.getLast();
				inicios.add(new InicioDisponibleDto(primero.getId(), primero.getHoraInicio(), ultimo.getHoraFin()));
			}
		}
		return inicios;
	}

	private static List<Horario> seleccionarConsecutivos(List<Horario> libres, int indiceInicio, int cantidad) {
		if (cantidad <= 0 || indiceInicio < 0 || indiceInicio + cantidad > libres.size()) {
			return List.of();
		}
		List<Horario> seleccionados = new ArrayList<>(cantidad);
		for (int indice = indiceInicio; indice < indiceInicio + cantidad; indice++) {
			Horario actual = libres.get(indice);
			if (actual.isBloqueado()
					|| (indice > indiceInicio
							&& !libres.get(indice - 1).getHoraFin().equals(actual.getHoraInicio()))) {
				return List.of();
			}
			seleccionados.add(actual);
		}
		return seleccionados;
	}

	private static int indiceBloque(List<Horario> libres, Long idHorario) {
		for (int indice = 0; indice < libres.size(); indice++) {
			if (libres.get(indice).getId().equals(idHorario)) {
				return indice;
			}
		}
		return -1;
	}
}
