package pe.edu.utp.medifast.util.converter;

import java.time.DayOfWeek;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Convierte {@code dia_semana} (1 = lunes … 7 = domingo) a {@link DayOfWeek}. */
@Converter
public class DiaSemanaConverter implements AttributeConverter<DayOfWeek, Integer> {

	@Override
	public Integer convertToDatabaseColumn(DayOfWeek dia) {
		return dia == null ? null : dia.getValue();
	}

	@Override
	public DayOfWeek convertToEntityAttribute(Integer valor) {
		return valor == null ? null : DayOfWeek.of(valor);
	}
}
