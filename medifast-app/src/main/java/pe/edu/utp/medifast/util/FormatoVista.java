package pe.edu.utp.medifast.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import pe.edu.utp.medifast.model.Medico;
import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.model.enums.TipoDocumentoEnum;

/**
 * Formatos de texto comunes a las pantallas del paciente (P2–P6), del médico (P7–P10) y al PDF de la confirmación,
 * en español de Perú.
 */
public final class FormatoVista {

	public static final Locale LOCALE = Locale.forLanguageTag("es-PE");
	public static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm", LOCALE);
	public static final DateTimeFormatter FECHA_INPUT = DateTimeFormatter.ofPattern("dd/MM/yyyy", LOCALE);
	private static final DateTimeFormatter FECHA_CORTA = DateTimeFormatter.ofPattern("EEE d MMM yyyy", LOCALE);
	private static final DateTimeFormatter DIA_CORTO = DateTimeFormatter.ofPattern("EEE d MMM", LOCALE);
	private static final DateTimeFormatter DIA_CORTO_COMA = DateTimeFormatter.ofPattern("EEE, d MMM", LOCALE);
	private static final DateTimeFormatter FECHA_LARGA = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy",
			LOCALE);

	private FormatoVista() {
	}

	/** "Lun 9 nov 2026". */
	public static String fechaCorta(LocalDate fecha) {
		return capitalizar(sinPuntos(FECHA_CORTA.format(fecha)));
	}

	/** "Mié 11 nov", sin el año (P2, P3). */
	public static String diaCorto(LocalDate fecha) {
		return capitalizar(sinPuntos(DIA_CORTO.format(fecha)));
	}

	/** "Mié, 11 nov", sin el año (P4). */
	public static String diaCortoConComa(LocalDate fecha) {
		return capitalizar(sinPuntos(DIA_CORTO_COMA.format(fecha)));
	}

	/** "Lun 9 nov 2026, 08:40". */
	public static String fechaHoraCorta(LocalDateTime fechaHora) {
		return fechaCorta(fechaHora.toLocalDate()) + ", " + HORA.format(fechaHora);
	}

	/** "Miércoles, 11 de noviembre de 2026". */
	public static String fechaLarga(LocalDate fecha) {
		return capitalizar(FECHA_LARGA.format(fecha));
	}

	/** "10:30 a. m." (P8). */
	public static String hora12(LocalTime hora) {
		return reloj12(hora) + " " + periodo(hora);
	}

	/** "10:30–11:30 a. m."; el periodo se repite solo si cambia, como en P2 y P5. */
	public static String rangoHoras12(LocalTime inicio, LocalTime fin) {
		return rangoHoras12(inicio, fin, "–");
	}

	/** "10:30 – 11:30 a. m.", con espacios alrededor del guion (resumen de P2 y PDF de la confirmación). */
	public static String rangoHoras12Espaciado(LocalTime inicio, LocalTime fin) {
		return rangoHoras12(inicio, fin, " – ");
	}

	private static String rangoHoras12(LocalTime inicio, LocalTime fin, String separador) {
		return periodo(inicio).equals(periodo(fin))
				? reloj12(inicio) + separador + reloj12(fin) + " " + periodo(fin)
				: hora12(inicio) + separador + hora12(fin);
	}

	/** "10:30–11:30" en formato de 24 horas. */
	public static String rangoHoras24(LocalTime inicio, LocalTime fin) {
		return HORA.format(inicio) + "–" + HORA.format(fin);
	}

	/** Minutos que dura la cita según sus bloques. */
	public static long minutos(VistaCita cita) {
		return Duration.between(cita.getHoraInicio(), cita.getHoraFin()).toMinutes();
	}

	/** "Nuevo · 60 min" (P4, P8). */
	public static String tipoYDuracion(VistaCita cita) {
		return EtiquetasEstado.tipo(cita.getTipoCita()) + " · " + minutos(cita) + " min";
	}

	/** "Lun 9 nov 2026, 10:12 a. m." (P8). */
	public static String fechaHora12(LocalDateTime fechaHora) {
		return fechaCorta(fechaHora.toLocalDate()) + ", " + hora12(fechaHora.toLocalTime());
	}

	private static String reloj12(LocalTime hora) {
		int hora12 = hora.getHour() % 12 == 0 ? 12 : hora.getHour() % 12;
		return "%d:%02d".formatted(hora12, hora.getMinute());
	}

	private static String periodo(LocalTime hora) {
		return hora.getHour() < 12 ? "a. m." : "p. m.";
	}

	/** "Del 9 al 14 nov 2026" o, si es un solo día, "15 nov 2026"; repite el mes o el año solo si cambian. */
	public static String rango(LocalDate desde, LocalDate hasta) {
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

	public static String mes(LocalDate fecha, TextStyle estilo) {
		return sinPuntos(fecha.getMonth().getDisplayName(estilo, LOCALE));
	}

	/** "#000118". */
	public static String numeroCita(Long idCita) {
		return "#%06d".formatted(idCita);
	}

	/** "DNI ••••3214": solo los datos mínimos (RNF07). */
	public static String documento(TipoDocumentoEnum tipo, String numero) {
		return tipo + " " + Enmascarador.documentoEnPantalla(numero);
	}

	/** "S/ 690.00"; "S/ 2,730.00" con separador de miles, como el formato del Excel. */
	public static String soles(BigDecimal monto) {
		return "S/ " + String.format(Locale.US, "%,.2f", monto.setScale(2, RoundingMode.HALF_UP));
	}

	/** Como {@link #soles(BigDecimal)}, pero "—" si aún no hay monto. */
	public static String solesOGuion(BigDecimal monto) {
		return monto == null ? "—" : soles(monto);
	}

	/** "1 cita" o "3 citas". */
	public static String cantidad(long total, String singular, String plural) {
		return total == 1 ? "1 " + singular : total + " " + plural;
	}

	/** "Dr. Moisés Barrantes": primer nombre y primer apellido, como en la navbar del diseño. */
	public static String nombreMedico(Medico medico) {
		return doctor(StringUtils.substringBefore(medico.getNombres(), " ") + " "
				+ StringUtils.substringBefore(medico.getApellidos(), " "));
	}

	/** "Dr. Moisés Andrés Barrantes Ríos": nombres y apellidos completos (P2, P5). */
	public static String nombreCompletoMedico(Medico medico) {
		return doctor(medico.getNombres() + " " + medico.getApellidos());
	}

	/** Antepone el tratamiento al nombre del médico tal como viene de {@code v_cita}. */
	public static String doctor(String nombre) {
		return "Dr. " + nombre;
	}

	/** "MB": inicial del primer nombre y del primer apellido. */
	public static String iniciales(String nombres, String apellidos) {
		return (primeraInicial(nombres) + primeraInicial(apellidos)).toUpperCase(Locale.ROOT);
	}

	private static String primeraInicial(String texto) {
		return texto.trim().split("\\s+")[0].substring(0, 1);
	}

	/**
	 * Inicial del primer nombre y del apellido paterno. {@code v_cita} trae "nombres apellidos" en un solo campo;
	 * con 3 palabras o más se toma la penúltima como apellido paterno ("Andrea Sofía Salazar Pérez" → "AS").
	 */
	public static String iniciales(String nombreCompleto) {
		String[] palabras = StringUtils.split(StringUtils.defaultString(nombreCompleto));
		if (palabras.length == 0) {
			return "?";
		}
		if (palabras.length == 1) {
			return palabras[0].substring(0, 1).toUpperCase(LOCALE);
		}
		String apellido = palabras.length >= 3 ? palabras[palabras.length - 2] : palabras[1];
		return (palabras[0].substring(0, 1) + apellido.substring(0, 1)).toUpperCase(LOCALE);
	}

	public static String sinPuntos(String texto) {
		return texto.replace(".", "");
	}

	public static String capitalizar(String texto) {
		return StringUtils.capitalize(texto);
	}
}
