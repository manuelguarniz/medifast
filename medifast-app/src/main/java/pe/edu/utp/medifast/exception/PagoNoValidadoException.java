package pe.edu.utp.medifast.exception;

/** Se intentó marcar como atendida una cita cuyo pago no está {@code VALIDADO} (RN-07, SD-06). */
public class PagoNoValidadoException extends AccionNoPermitidaException {

	private static final long serialVersionUID = 1L;

	public PagoNoValidadoException() {
		super("Primero valida el pago.");
	}
}
