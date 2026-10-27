package pe.edu.utp.medifast.util;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;
import pe.edu.utp.medifast.model.enums.EstadoPagoEnum;
import pe.edu.utp.medifast.model.enums.TipoCitaEnum;

/** Etiquetas en pantalla de los estados guardados en la BD (README §5.2) y del tipo de cita (RN-02). */
public final class EtiquetasEstado {

	/** Una cita {@code PENDIENTE_PAGO} cuyo plazo pasó y que RF16 aún no canceló (RF10). */
	public static final String VENCIDA = "Vencida";

	private static final Map<EstadoCitaEnum, String> CITA = ImmutableMap.of(
			EstadoCitaEnum.PENDIENTE_PAGO, "Pago pendiente",
			EstadoCitaEnum.PROGRAMADA, "Confirmada",
			EstadoCitaEnum.ATENDIDA, "Atendida",
			EstadoCitaEnum.NO_ASISTIO, "No asistió",
			EstadoCitaEnum.CANCELADA, "Cancelada");

	private static final Map<EstadoPagoEnum, String> PAGO = ImmutableMap.of(
			EstadoPagoEnum.POR_VALIDAR, "Por validar",
			EstadoPagoEnum.VALIDADO, "Verificado",
			EstadoPagoEnum.RECHAZADO, "Rechazado");

	private EtiquetasEstado() {
	}

	public static String cita(EstadoCitaEnum estado) {
		return CITA.get(estado);
	}

	public static String pago(EstadoPagoEnum estado) {
		return estado == null ? "—" : PAGO.get(estado);
	}

	public static String tipo(TipoCitaEnum tipo) {
		return tipo == TipoCitaEnum.NUEVO ? "Nuevo" : "Control";
	}

	/** Sufijo de la clase {@code citas-badge-*} que da el color del badge de la cita. */
	public static String claseCita(EstadoCitaEnum estado) {
		return switch (estado) {
			case PENDIENTE_PAGO -> "pendiente";
			case PROGRAMADA -> "confirmada";
			case ATENDIDA -> "atendida";
			case NO_ASISTIO -> "no-asistio";
			case CANCELADA -> "cancelada";
		};
	}

	/** Sufijo de la clase {@code citas-badge-*} del pago; {@code null} si la cita aún no tiene pago. */
	public static String clasePago(EstadoPagoEnum estado) {
		if (estado == null) {
			return null;
		}
		return switch (estado) {
			case POR_VALIDAR -> "pendiente";
			case VALIDADO -> "atendida";
			case RECHAZADO -> "cancelada";
		};
	}
}
