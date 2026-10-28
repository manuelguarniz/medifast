package pe.edu.utp.medifast.exception;

import java.util.Map;

/**
 * La constancia o el n.º de operación no cumplen RF03, RN-11 o RNF06. Lleva un mensaje por campo
 * ({@code comprobante}, {@code nroOperacion}) para mostrarlos todos a la vez en P3.
 */
public class PagoInvalidoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final transient Map<String, String> errores;

	public PagoInvalidoException(Map<String, String> errores) {
		super(String.join(" ", errores.values()));
		this.errores = Map.copyOf(errores);
	}

	public Map<String, String> getErrores() {
		return errores;
	}
}
