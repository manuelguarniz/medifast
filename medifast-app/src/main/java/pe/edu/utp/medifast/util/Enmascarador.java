package pe.edu.utp.medifast.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Enmascara datos personales antes de escribirlos en logs (RNF07, Ley 29733): de documentos y celulares solo
 * quedan los 4 últimos caracteres; de los correos, la primera letra y el dominio.
 */
public final class Enmascarador {

	private static final int VISIBLES = 4;
	private static final char MASCARA = '*';
	private static final String SIN_DATO = "-";

	/** DNI (8), celular (9) o CE numérico (9 a 12) dentro de un texto libre. */
	private static final Pattern DIGITOS_EN_TEXTO = Pattern.compile("(?<![0-9A-Za-z])\\d{8,12}(?![0-9A-Za-z])");

	private static final Pattern CORREO_EN_TEXTO = Pattern
			.compile("([A-Za-z0-9._%+-])[A-Za-z0-9._%+-]*(@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})");

	private Enmascarador() {
	}

	/** {@code 45678912 → ****8912}; {@code 001234567AB → *******67AB}. */
	public static String documento(String numeroDocumento) {
		return ocultarMenosUltimos(numeroDocumento);
	}

	/**
	 * Documento abreviado para pantalla, con los datos mínimos (RNF07): {@code 45678912 → ••••8912}. A diferencia
	 * de {@link #documento(String)}, no revela la longitud del documento.
	 */
	public static String documentoEnPantalla(String numeroDocumento) {
		if (StringUtils.isBlank(numeroDocumento)) {
			return SIN_DATO;
		}
		String limpio = numeroDocumento.strip();
		return limpio.length() <= VISIBLES ? "••••" : "••••" + StringUtils.right(limpio, VISIBLES);
	}

	/** {@code 987654321 → *****4321}. */
	public static String celular(String celular) {
		return ocultarMenosUltimos(celular);
	}

	/** {@code juan.perez@gmail.com → j***@gmail.com}. */
	public static String correo(String correo) {
		if (StringUtils.isBlank(correo)) {
			return SIN_DATO;
		}
		int arroba = correo.indexOf('@');
		if (arroba <= 0) {
			return StringUtils.repeat(MASCARA, 3);
		}
		return correo.charAt(0) + StringUtils.repeat(MASCARA, 3) + correo.substring(arroba);
	}

	/**
	 * Enmascara los documentos, celulares y correos que aparezcan en un texto libre. Es la red de seguridad que
	 * aplica Logback a todos los mensajes; un CE con letras no se reconoce aquí, por eso se enmascara siempre con
	 * {@link #documento(String)} antes de registrarlo.
	 */
	public static String texto(String mensaje) {
		if (StringUtils.isEmpty(mensaje)) {
			return mensaje;
		}
		String resultado = DIGITOS_EN_TEXTO.matcher(mensaje).replaceAll(m -> ocultarMenosUltimos(m.group()));
		return CORREO_EN_TEXTO.matcher(resultado)
				.replaceAll(m -> Matcher.quoteReplacement(m.group(1) + StringUtils.repeat(MASCARA, 3) + m.group(2)));
	}

	private static String ocultarMenosUltimos(String valor) {
		if (StringUtils.isBlank(valor)) {
			return SIN_DATO;
		}
		String limpio = valor.strip();
		if (limpio.length() <= VISIBLES) {
			return StringUtils.repeat(MASCARA, limpio.length());
		}
		return StringUtils.repeat(MASCARA, limpio.length() - VISIBLES) + StringUtils.right(limpio, VISIBLES);
	}
}
