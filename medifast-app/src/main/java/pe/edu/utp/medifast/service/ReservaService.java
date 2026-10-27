package pe.edu.utp.medifast.service;

import java.time.LocalDate;
import pe.edu.utp.medifast.dto.validacion.DisponibilidadDto;
import pe.edu.utp.medifast.model.Cita;
import pe.edu.utp.medifast.model.enums.TipoCitaEnum;

/**
 * Servicio para la consulta de disponibilidad y creación transaccional de reservas (RF01, RF05).
 */
public interface ReservaService {

    /**
     * Calcula los horarios disponibles y la tarifa para un paciente en una fecha dada.
     */
    DisponibilidadDto disponibilidad(Long idPaciente, Long idMedico, LocalDate fecha);

    /**
     * Detecta si la cita es NUEVO o CONTROL según el historial de atención (RN-02).
     */
    TipoCitaEnum detectarTipo(Long idPaciente, Long idMedico);

    /**
     * Realiza el apartado transaccional de la cita por 15 minutos (RN-01, RN-04, RN-09).
     */
    Cita reservar(Long idPaciente, Long idHorario);
}
