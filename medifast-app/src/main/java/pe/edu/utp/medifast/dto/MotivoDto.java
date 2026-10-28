package pe.edu.utp.medifast.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Motivo obligatorio al rechazar el pago o cancelar la cita (RN-13); {@code cita.observacion} es VARCHAR(500).
 *
 * @param observacion motivo que el paciente verá en "Mis citas"
 */
public record MotivoDto(
		@NotBlank(message = "Ingresa el motivo.")
		@Size(max = MotivoDto.MAXIMO, message = "El motivo no puede superar los 500 caracteres.")
		String observacion) {

	public static final int MAXIMO = 500;
}
