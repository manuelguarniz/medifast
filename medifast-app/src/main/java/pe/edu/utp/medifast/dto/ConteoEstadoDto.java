package pe.edu.utp.medifast.dto;

import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;

/**
 * Cantidad de citas en un estado; una fila del {@code GROUP BY estado} del reporte de citas (P9).
 *
 * @param estado estado de la cita
 * @param cantidad citas en ese estado
 */
public record ConteoEstadoDto(EstadoCitaEnum estado, long cantidad) {
}
