package pe.edu.utp.medifast.model.enums;

/** Ciclo de vida de una cita (RN-02, RN-05). PENDIENTE_PAGO y PROGRAMADA cuentan como activas (RN-09). */
public enum EstadoCitaEnum {
	PENDIENTE_PAGO,
	PROGRAMADA,
	ATENDIDA,
	NO_ASISTIO,
	CANCELADA
}
