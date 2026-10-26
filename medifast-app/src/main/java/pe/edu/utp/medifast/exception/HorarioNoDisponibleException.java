package pe.edu.utp.medifast.exception;

/** El horario dejó de estar disponible antes de completar la reserva (RN-01). */
public class HorarioNoDisponibleException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public HorarioNoDisponibleException() {
		super("Horario no disponible, elija otro.");
	}
}
