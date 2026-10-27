package pe.edu.utp.medifast.dto;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;

/**
 * Filtros de la agenda del médico (P7, RF04): un día o la semana (lunes a domingo) que contiene la fecha, y un
 * estado opcional de la cita.
 *
 * @param vista día o semana
 * @param fecha día elegido; en la vista semana, cualquier día de esa semana
 * @param estado estado de la cita; {@code null} = todos
 */
public record FiltroAgendaDto(Vista vista, LocalDate fecha, EstadoCitaEnum estado) {

	/** Período que muestra la agenda. */
	public enum Vista {
		DIA, SEMANA
	}

	public FiltroAgendaDto {
		checkNotNull(vista, "La vista es obligatoria");
		checkNotNull(fecha, "La fecha es obligatoria");
	}

	/** Primer día del período: la fecha o el lunes de su semana. */
	public LocalDate desde() {
		return vista == Vista.DIA ? fecha : fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
	}

	/** Último día del período: la fecha o el domingo de su semana. */
	public LocalDate hasta() {
		return vista == Vista.DIA ? fecha : desde().plusDays(6);
	}
}
