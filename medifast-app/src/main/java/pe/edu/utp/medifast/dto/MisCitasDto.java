package pe.edu.utp.medifast.dto;

import java.util.List;
import java.util.Optional;

/**
 * Citas del paciente repartidas como en P5: la activa ("Próximas", como máximo una por RN-09) y el resto
 * ("Historial": atendidas, no asistió, canceladas y vencidas), de la más reciente a la más antigua (RF10).
 *
 * @param proxima cita {@code PROGRAMADA} o {@code PENDIENTE_PAGO} vigente, si existe
 * @param historial demás citas, ordenadas por fecha y hora descendentes
 */
public record MisCitasDto(Optional<CitaPacienteDto> proxima, List<CitaPacienteDto> historial) {
}
