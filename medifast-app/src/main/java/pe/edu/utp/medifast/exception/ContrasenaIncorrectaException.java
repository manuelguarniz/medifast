package pe.edu.utp.medifast.exception;

/** Indica que la contraseña actual ingresada al cambiarla no coincide con la registrada. */
public class ContrasenaIncorrectaException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ContrasenaIncorrectaException() {
		super("La contraseña actual no es correcta.");
	}
}
