package pe.edu.utp.medifast.dto;

import java.math.BigDecimal;

import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;

/**
 * Cita del historial del paciente (P5, RF10) con lo que la vista no trae: el monto de la tarifa cuando aún no hay
 * pago y si la reserva ya venció sin que RF16 la cancele.
 *
 * @param cita fila de {@code v_cita}
 * @param monto monto del pago o, si no existe, de la tarifa vigente
 * @param vencida {@code PENDIENTE_PAGO} con {@code expira_en} ya pasado
 * @param segundosRestantes tiempo para adjuntar la constancia; 0 si no aplica
 */
public record CitaPacienteDto(VistaCita cita, BigDecimal monto, boolean vencida, long segundosRestantes) {

	/** "Continuar pago": reserva pendiente que aún no vence (RF03). */
	public boolean pagoPendiente() {
		return cita.getEstado() == EstadoCitaEnum.PENDIENTE_PAGO && !vencida;
	}

	/** "Ver detalle" y "Descargar PDF": solo citas confirmadas o atendidas (RF10, RF15). */
	public boolean permiteDetalle() {
		return cita.getEstado() == EstadoCitaEnum.PROGRAMADA || cita.getEstado() == EstadoCitaEnum.ATENDIDA;
	}

	/** "Ver constancia": la cita tiene un pago registrado (RF12). */
	public boolean tieneConstancia() {
		return cita.getIdPago() != null;
	}
}
