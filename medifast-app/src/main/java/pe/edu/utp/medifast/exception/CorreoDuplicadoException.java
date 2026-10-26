package pe.edu.utp.medifast.exception;

/** Indica que el correo ya pertenece a un paciente. */
public class CorreoDuplicadoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public CorreoDuplicadoException() {
		super("Este correo ya está registrado.");
	}
}
