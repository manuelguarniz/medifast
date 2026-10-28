package pe.edu.utp.medifast.exception;

/** Pasaron los 15 minutos del apartado (RN-04, RF16): la reserva ya no admite la constancia. */
public class ReservaVencidaException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ReservaVencidaException() {
		super("Tiempo de reserva vencido");
	}
}
