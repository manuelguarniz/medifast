package pe.edu.utp.medifast.controller;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Enums;

import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;

/**
 * Lectura tolerante de los filtros que llegan como texto en la URL (P7–P10): lo que no es válido se devuelve como
 * {@code null} y cada pantalla decide su valor por defecto o su mensaje de error.
 */
final class Parametros {

	private Parametros() {
	}

	/** Fecha ISO ("2026-11-09"); {@code null} si viene vacía o con formato inválido. */
	static LocalDate fecha(String texto) {
		if (StringUtils.isBlank(texto)) {
			return null;
		}
		try {
			return LocalDate.parse(texto.strip());
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	/** Estado de la cita por su nombre ("ATENDIDA"); {@code null} si no corresponde a ninguno. */
	static EstadoCitaEnum estadoCita(String texto) {
		return Enums.getIfPresent(EstadoCitaEnum.class, StringUtils.defaultString(texto)).orNull();
	}
}
