package pe.edu.utp.medifast.exception;

/** La fecha solicitada está fuera de la ventana de reserva configurada (RN-06). */
public class FueraDeRangoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public FueraDeRangoException() {
		super("La fecha está fuera del rango permitido para reservar.");
	}
}
