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
import pe.edu.utp.medifast.dto.validacion.ContrasenasCoinciden;
import pe.edu.utp.medifast.dto.validacion.DocumentoValido;
import pe.edu.utp.medifast.dto.validacion.LongitudBcryptValida;
import pe.edu.utp.medifast.model.enums.TipoDocumentoEnum;

/** Datos de registro de paciente validados antes de persistirlos. */
@DocumentoValido
@ContrasenasCoinciden
public record RegistroDto(
		@NotNull(message = "Selecciona el tipo de documento.") TipoDocumentoEnum tipoDocumento,
		@NotBlank(message = "Ingresa tu número de documento.") String numeroDocumento,
		@NotBlank(message = "Ingresa tus nombres.") @Size(max = 100, message = "Tus nombres no pueden superar 100 caracteres.") String nombres,
		@NotBlank(message = "Ingresa tus apellidos.") @Size(max = 100, message = "Tus apellidos no pueden superar 100 caracteres.") String apellidos,
		@NotNull(message = "Ingresa una fecha de nacimiento anterior a hoy.") @Past(message = "Ingresa una fecha de nacimiento anterior a hoy.") LocalDate fechaNacimiento,
		@NotBlank(message = "Ingresa 9 dígitos que empiecen con 9.") @Pattern(regexp = "^9\\d{8}$", message = "Ingresa 9 dígitos que empiecen con 9.") String celular,
		@NotBlank(message = "Ingresa un correo válido.") @Email(message = "Ingresa un correo válido.") @Size(max = 150, message = "El correo no puede superar 150 caracteres.") String correo,
		@NotBlank(message = "Ingresa una contraseña.") @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres.") @LongitudBcryptValida String contrasena,
		@NotBlank(message = "Confirma tu contraseña.") String confirmacion) {

	public static RegistroDto vacio() {
		return new RegistroDto(TipoDocumentoEnum.DNI, "", "", "", null, "", "", "", "");
	}

	/** Devuelve el documento sin espacios y en mayúsculas para guardarlo y buscarlo de forma canónica. */
	public String numeroDocumentoNormalizado() {
		return StringUtils.upperCase(StringUtils.deleteWhitespace(numeroDocumento), Locale.ROOT);
	}
}
