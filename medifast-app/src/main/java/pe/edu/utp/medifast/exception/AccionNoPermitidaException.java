package pe.edu.utp.medifast.exception;

/**
 * El estado de la cita o del pago no permite la acción del médico en P8 (RN-05, RN-14), por ejemplo marcar la
 * asistencia antes de la hora de inicio o validar un pago que ya se validó.
 */
public class AccionNoPermitidaException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public AccionNoPermitidaException(String mensaje) {
		super(mensaje);
	}
}
