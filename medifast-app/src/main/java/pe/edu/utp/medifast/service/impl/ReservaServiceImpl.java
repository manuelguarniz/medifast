package pe.edu.utp.medifast.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.utp.medifast.dao.CitaDao;
import pe.edu.utp.medifast.dao.HorarioDao;
import pe.edu.utp.medifast.dao.PacienteDao;
import pe.edu.utp.medifast.dao.TarifaDao;
import pe.edu.utp.medifast.dao.VistaCitaDao;
import pe.edu.utp.medifast.dto.validacion.DisponibilidadDto;
import pe.edu.utp.medifast.dto.validacion.InicioDisponibleDto;
import pe.edu.utp.medifast.exception.CitaActivaException;
import pe.edu.utp.medifast.exception.FueraDeRangoException;
import pe.edu.utp.medifast.exception.HorarioNoDisponibleException;
import pe.edu.utp.medifast.model.Cita;
import pe.edu.utp.medifast.model.Horario;
import pe.edu.utp.medifast.model.Medico;
import pe.edu.utp.medifast.model.Paciente;
import pe.edu.utp.medifast.model.Tarifa;
import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;
import pe.edu.utp.medifast.model.enums.TipoCitaEnum;
import pe.edu.utp.medifast.service.ReservaService;
import pe.edu.utp.medifast.util.Auditoria;

@Service
public class ReservaServiceImpl implements ReservaService {

    private static final int MINUTOS_BLOQUE = 30;
    private static final int MINUTOS_APARTADO = 15;
    private static final int DIAS_ANTICIPACION_MIN = 1;
    private static final int DIAS_ANTICIPACION_MAX = 30;

    private final CitaDao citaDao;
    private final HorarioDao horarioDao;
    private final TarifaDao tarifaDao;
    private final PacienteDao pacienteDao;
    private final VistaCitaDao vistaCitaDao;
    private final Auditoria auditoria;
    private final Clock clock;

    public ReservaServiceImpl(
            CitaDao citaDao,
            HorarioDao horarioDao,
            TarifaDao tarifaDao,
            PacienteDao pacienteDao,
            VistaCitaDao vistaCitaDao,
            Auditoria auditoria,
            Clock clock) {
        this.citaDao = citaDao;
        this.horarioDao = horarioDao;
        this.tarifaDao = tarifaDao;
        this.pacienteDao = pacienteDao;
        this.vistaCitaDao = vistaCitaDao;
        this.auditoria = auditoria;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public DisponibilidadDto disponibilidad(Long idPaciente, Long idMedico, LocalDate fecha) {
        LocalDate hoy = LocalDate.now(clock);

        if (fecha.isBefore(hoy.plusDays(DIAS_ANTICIPACION_MIN)) || fecha.isAfter(hoy.plusDays(DIAS_ANTICIPACION_MAX))) {
            throw new FueraDeRangoException("La fecha está fuera de la ventana de reserva permitida (1 a 30 días).");
        }

        List<Horario> libres = horarioDao.bloquesLibres(idMedico, fecha);
        
        TipoCitaEnum tipo = detectarTipo(idPaciente, idMedico);
        Tarifa tarifa = tarifaDao.findByMedicoIdAndTipoCita(idMedico, tipo)
            .orElseThrow(() -> new IllegalArgumentException("Tarifa no configurada para el médico"));

        int bloquesNecesarios = tarifa.getDuracionMin() / MINUTOS_BLOQUE;
        List<InicioDisponibleDto> inicios = calcularIniciosValidos(libres, bloquesNecesarios);

        // Búsqueda tipo-safe de cita activa usando enums y mapeando la vista
        List<EstadoCitaEnum> estadosActivos = List.of(EstadoCitaEnum.PENDIENTE_PAGO, EstadoCitaEnum.PROGRAMADA);
        Optional<Cita> citaEntidad = citaDao.findFirstByPacienteIdAndEstadoIn(idPaciente, estadosActivos);
        Optional<VistaCita> citaActiva = citaEntidad.flatMap(c -> vistaCitaDao.findById(c.getId()));

        return new DisponibilidadDto(
            tipo,
            tarifa.getDuracionMin(),
            tarifa.getPrecio(),
            hoy.plusDays(DIAS_ANTICIPACION_MIN),
            hoy.plusDays(DIAS_ANTICIPACION_MAX),
            fecha,
            inicios,
            citaActiva
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TipoCitaEnum detectarTipo(Long idPaciente, Long idMedico) {
        boolean tieneAtendida = citaDao.existsByPacienteIdAndMedicoIdAndEstado(idPaciente, idMedico, EstadoCitaEnum.ATENDIDA);
        return tieneAtendida ? TipoCitaEnum.CONTROL : TipoCitaEnum.NUEVO;
    }

    @Override
    @Transactional
    public Cita reservar(Long idPaciente, Long idHorario) {
        citaDao.findFirstByPacienteIdAndEstadoIn(idPaciente, List.of(EstadoCitaEnum.PENDIENTE_PAGO, EstadoCitaEnum.PROGRAMADA))
            .ifPresent(c -> {
                VistaCita vista = vistaCitaDao.findById(c.getId()).orElse(null);
                throw new CitaActivaException("El paciente ya posee una cita activa.", vista);
            });

        Horario hInicio = horarioDao.findById(idHorario)
            .orElseThrow(() -> new HorarioNoDisponibleException("El bloque de horario no existe."));

        Medico medico = hInicio.getMedico();
        LocalDate hoy = LocalDate.now(clock);

        if (hInicio.getFecha().isBefore(hoy.plusDays(DIAS_ANTICIPACION_MIN)) ||
            hInicio.getFecha().isAfter(hoy.plusDays(DIAS_ANTICIPACION_MAX))) {
            throw new FueraDeRangoException("Fecha fuera del rango permitido.");
        }

        if (hInicio.isBloqueado()) {
            throw new HorarioNoDisponibleException("El horario seleccionado está bloqueado.");
        }

        TipoCitaEnum tipo = detectarTipo(idPaciente, medico.getId());
        Tarifa tarifa = tarifaDao.findByMedicoIdAndTipoCita(medico.getId(), tipo)
            .orElseThrow(() -> new IllegalArgumentException("Tarifa no configurada."));

        int bloquesNecesarios = tarifa.getDuracionMin() / MINUTOS_BLOQUE;
        List<Horario> bloquesTomados = obtenerBloquesConsecutivos(medico.getId(), hInicio.getFecha(), hInicio, bloquesNecesarios);

        Paciente paciente = pacienteDao.findById(idPaciente)
            .orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado."));

        LocalDateTime expiraEn = LocalDateTime.now(clock).plusMinutes(MINUTOS_APARTADO);
        Cita cita = new Cita(paciente, medico, tipo, expiraEn);

        for (Horario b : bloquesTomados) {
            cita.agregarBloque(b);
        }

        try {
            Cita guardada = citaDao.saveAndFlush(cita);
            auditoria.reserva(guardada.getId());
            return guardada;
        } catch (DataIntegrityViolationException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("uq_cita_paciente_activo")) {
                throw new CitaActivaException("Ya cuenta con una cita activa.", null);
            }
            if (msg.contains("uq_cita_horario_activo")) {
                throw new HorarioNoDisponibleException("El horario ya fue reservado por otro paciente.");
            }
            throw e;
        }
    }

    private List<InicioDisponibleDto> calcularIniciosValidos(List<Horario> libres, int n) {
        List<InicioDisponibleDto> result = new ArrayList<>();
        for (int i = 0; i <= libres.size() - n; i++) {
            boolean esConsecutivo = true;
            for (int j = 0; j < n - 1; j++) {
                if (!libres.get(i + j).getHoraFin().equals(libres.get(i + j + 1).getHoraInicio())) {
                    esConsecutivo = false;
                    break;
                }
            }
            if (esConsecutivo) {
                Horario primero = libres.get(i);
                Horario ultimo = libres.get(i + n - 1);
                result.add(new InicioDisponibleDto(primero.getId(), primero.getHoraInicio(), ultimo.getHoraFin()));
            }
        }
        return result;
    }

    private List<Horario> obtenerBloquesConsecutivos(Long idMedico, LocalDate fecha, Horario hInicio, int n) {
        List<Horario> libres = horarioDao.bloquesLibres(idMedico, fecha);
        int idx = libres.indexOf(hInicio);
        if (idx == -1 || idx + n > libres.size()) {
            throw new HorarioNoDisponibleException("El horario ya no se encuentra disponible.");
        }

        List<Horario> tomados = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Horario actual = libres.get(idx + i);
            if (i > 0) {
                Horario previo = libres.get(idx + i - 1);
                if (!previo.getHoraFin().equals(actual.getHoraInicio())) {
                    throw new HorarioNoDisponibleException("No existen bloques consecutivos suficientes.");
                }
            }
            tomados.add(actual);
        }
        return tomados;
    }
}