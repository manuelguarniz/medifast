package pe.edu.utp.medifast.dto;

import java.time.LocalDate;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.edu.utp.medifast.model.Paciente;

/** Datos personales editables del paciente en "Mi perfil" (RF11); el documento no forma parte (RN-12). */
public record PerfilDto(
		@NotBlank(message = "Ingresa tus nombres.") @Size(max = 100, message = "Tus nombres no pueden superar 100 caracteres.") String nombres,
		@NotBlank(message = "Ingresa tus apellidos.") @Size(max = 100, message = "Tus apellidos no pueden superar 100 caracteres.") String apellidos,
		@NotNull(message = "Ingresa una fecha de nacimiento anterior a hoy.") @Past(message = "Ingresa una fecha de nacimiento anterior a hoy.") LocalDate fechaNacimiento,
		@NotBlank(message = "Ingresa 9 dígitos que empiecen con 9.") @Pattern(regexp = "^9\\d{8}$", message = "Ingresa 9 dígitos que empiecen con 9.") String celular,
		@NotBlank(message = "Ingresa un correo válido.") @Email(regexp = ".+@[^@]+\\.[^@.]+$", message = "Ingresa un correo válido.") @Size(max = 150, message = "El correo no puede superar 150 caracteres.") String correo) {

	public static PerfilDto desde(Paciente paciente) {
		return new PerfilDto(paciente.getNombres(), paciente.getApellidos(), paciente.getFechaNacimiento(),
				paciente.getCelular(), paciente.getCorreo());
	}

	/** Devuelve los datos con los mismos criterios de limpieza que el registro (espacios y correo en minúsculas). */
	public PerfilDto normalizado() {
		return new PerfilDto(StringUtils.normalizeSpace(nombres), StringUtils.normalizeSpace(apellidos),
				fechaNacimiento, StringUtils.trim(celular), StringUtils.lowerCase(StringUtils.trim(correo), Locale.ROOT));
	}
}
