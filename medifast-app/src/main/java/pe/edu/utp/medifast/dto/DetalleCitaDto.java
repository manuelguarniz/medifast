package pe.edu.utp.medifast.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import pe.edu.utp.medifast.model.Pago;
import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;
import pe.edu.utp.medifast.model.enums.EstadoPagoEnum;

/**
 * Cita para P8 (detalle del médico) con las acciones que permite su estado (RF08, RF13, RF14, RN-07, RN-14).
 *
 * @param cita fila de {@code v_cita}: paciente, fecha, horas, estado, pago y motivo de la cancelación
 * @param celular celular del paciente
 * @param correo correo del paciente
 * @param monto monto del pago o, mientras no lo hay, la tarifa vigente del tipo de cita (RN-03)
 * @param pago pago con la constancia; {@code null} mientras la cita está {@code PENDIENTE_PAGO}
 * @param iniciada si la hora de inicio ya llegó (RN-14)
 * @param actualizadaEn último cambio de la cita: momento en que se marcó atendida, no asistió o se canceló
 */
public record DetalleCitaDto(VistaCita cita, String celular, String correo, BigDecimal monto, Pago pago, boolean iniciada,
		LocalDateTime actualizadaEn) {

	/** RF13: validar o rechazar solo sobre pagos {@code POR_VALIDAR} de citas confirmadas. */
	public boolean pagoPorValidar() {
		return estado() == EstadoCitaEnum.PROGRAMADA && pago != null
				&& pago.getEstado() == EstadoPagoEnum.POR_VALIDAR;
	}

	/** La sección "Asistencia" solo existe en las citas confirmadas (RF08). */
	public boolean admiteAsistencia() {
		return estado() == EstadoCitaEnum.PROGRAMADA;
	}

	/** RN-07 y RN-14: confirmada, pago validado y hora de inicio alcanzada. */
	public boolean puedeMarcarAtendida() {
		return admiteAsistencia() && iniciada && pagoValidado();
	}

	/** RN-14: confirmada y hora de inicio alcanzada; no exige el pago. */
	public boolean puedeMarcarNoAsistio() {
		return admiteAsistencia() && iniciada;
	}

	/** RF14: solo las citas activas se cancelan. */
	public boolean puedeCancelar() {
		return estado() == EstadoCitaEnum.PENDIENTE_PAGO || estado() == EstadoCitaEnum.PROGRAMADA;
	}

	public boolean pagoValidado() {
		return pago != null && pago.getEstado() == EstadoPagoEnum.VALIDADO;
	}

	/** Atendida, no asistió o cancelada: la card de acciones solo informa. */
	public boolean finalizada() {
		return !puedeCancelar();
	}

	private EstadoCitaEnum estado() {
		return cita.getEstado();
	}
}
