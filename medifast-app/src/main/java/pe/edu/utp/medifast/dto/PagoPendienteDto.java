package pe.edu.utp.medifast.dto;

import java.math.BigDecimal;

import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.model.enums.CanceladaPorEnum;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;

/**
 * Cita que el paciente abre en P3 (RF03).
 *
 * @param cita fila de {@code v_cita}
 * @param monto tarifa vigente del tipo de cita (RN-03)
 * @param segundosRestantes tiempo para adjuntar la constancia; 0 si ya venció o no aplica
 */
public record PagoPendienteDto(VistaCita cita, BigDecimal monto, long segundosRestantes) {

	/** Reserva {@code PENDIENTE_PAGO} dentro del plazo: se muestra el formulario. */
	public boolean pendiente() {
		return cita.getEstado() == EstadoCitaEnum.PENDIENTE_PAGO && segundosRestantes > 0;
	}

	/**
	 * Venció el plazo: la reserva sigue {@code PENDIENTE_PAGO} porque RF16 aún no pasa, o RF16 ya la canceló. Se
	 * muestra el modal "Tiempo de reserva vencido".
	 */
	public boolean vencida() {
		return cita.getEstado() == EstadoCitaEnum.PENDIENTE_PAGO
				? segundosRestantes <= 0
				: cita.getEstado() == EstadoCitaEnum.CANCELADA && cita.getCanceladaPor() == CanceladaPorEnum.SISTEMA;
	}

	/** La constancia ya se envió: P3 lleva a la confirmación (P4). */
	public boolean pagada() {
		return cita.getEstado() == EstadoCitaEnum.PROGRAMADA || cita.getEstado() == EstadoCitaEnum.ATENDIDA;
	}
}
