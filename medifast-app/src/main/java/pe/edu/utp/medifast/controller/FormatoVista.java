package pe.edu.utp.medifast.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import pe.edu.utp.medifast.model.Medico;
import pe.edu.utp.medifast.model.enums.TipoDocumentoEnum;
import pe.edu.utp.medifast.util.Enmascarador;

/** Formatos de texto comunes a las pantallas del médico (P7, P9 y P10), en español de Perú. */
final class FormatoVista {

	static final Locale LOCALE = Locale.forLanguageTag("es-PE");
	static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm", LOCALE);
	static final DateTimeFormatter FECHA_INPUT = DateTimeFormatter.ofPattern("dd/MM/yyyy", LOCALE);
	private static final DateTimeFormatter FECHA_CORTA = DateTimeFormatter.ofPattern("EEE d MMM yyyy", LOCALE);

	private FormatoVista() {
	}

	/** "Lun 9 nov 2026". */
	static String fechaCorta(LocalDate fecha) {
		return capitalizar(sinPuntos(FECHA_CORTA.format(fecha)));
	}

	/** "Lun 9 nov 2026, 08:40". */
	static String fechaHoraCorta(LocalDateTime fechaHora) {
		return fechaCorta(fechaHora.toLocalDate()) + ", " + HORA.format(fechaHora);
	}

	/** "Del 9 al 14 nov 2026" o, si es un solo día, "15 nov 2026"; repite el mes o el año solo si cambian. */
	static String rango(LocalDate desde, LocalDate hasta) {
		String fin = hasta.getDayOfMonth() + " " + mes(hasta, TextStyle.SHORT) + " " + hasta.getYear();
		if (desde.equals(hasta)) {
			return fin;
		}
		String inicio = desde.getDayOfMonth()
				+ (desde.getMonth() == hasta.getMonth() && desde.getYear() == hasta.getYear()
						? ""
						: " " + mes(desde, TextStyle.SHORT))
				+ (desde.getYear() == hasta.getYear() ? "" : " " + desde.getYear());
		return "Del " + inicio + " al " + fin;
	}

	static String mes(LocalDate fecha, TextStyle estilo) {
		return sinPuntos(fecha.getMonth().getDisplayName(estilo, LOCALE));
	}

	/** "#000118". */
	static String numeroCita(Long idCita) {
		return "#%06d".formatted(idCita);
	}

	/** "DNI ••••3214": solo los datos mínimos (RNF07). */
	static String documento(TipoDocumentoEnum tipo, String numero) {
		return tipo + " " + Enmascarador.documentoEnPantalla(numero);
	}

	/** "S/ 690.00"; "S/ 2,730.00" con separador de miles, como el formato del Excel. */
	static String soles(BigDecimal monto) {
		return "S/ " + String.format(Locale.US, "%,.2f", monto.setScale(2, RoundingMode.HALF_UP));
	}

	/** "Dr. Moisés Barrantes": primer nombre y primer apellido, como en la navbar del diseño. */
	static String nombreMedico(Medico medico) {
		return "Dr. " + StringUtils.substringBefore(medico.getNombres(), " ") + " "
				+ StringUtils.substringBefore(medico.getApellidos(), " ");
	}

	static String sinPuntos(String texto) {
		return texto.replace(".", "");
	}

	static String capitalizar(String texto) {
		return StringUtils.capitalize(texto);
	}
}
