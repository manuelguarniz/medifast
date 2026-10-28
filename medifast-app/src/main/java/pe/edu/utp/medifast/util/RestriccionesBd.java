package pe.edu.utp.medifast.util;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;

/**
 * Identifica qué restricción única de la BD rechazó un INSERT o UPDATE, para traducirla a la excepción de negocio
 * (documento o correo duplicado, cita activa, horario ocupado) cuando dos peticiones compiten (RNF05).
 */
public final class RestriccionesBd {

	private RestriccionesBd() {
	}

	/** {@code true} si algún mensaje de la cadena de causas nombra la restricción, sin distinguir mayúsculas. */
	public static boolean viola(DataIntegrityViolationException ex, String restriccion) {
		for (Throwable causa = ex; causa != null; causa = causa.getCause()) {
			String mensaje = causa.getMessage();
			if (mensaje != null && mensaje.toLowerCase(Locale.ROOT).contains(restriccion)) {
				return true;
			}
		}
		return false;
	}
}
