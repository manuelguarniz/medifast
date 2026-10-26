package pe.edu.utp.medifast.exception;

/** Indica que el documento ya pertenece a una cuenta. */
public class DocumentoDuplicadoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public DocumentoDuplicadoException() {
		super("Este número de documento ya está registrado.");
	}
}
