package pe.edu.utp.medifast.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.utp.medifast.dto.validacion.ContrasenaConfirmada;
import pe.edu.utp.medifast.dto.validacion.ContrasenasCoinciden;
import pe.edu.utp.medifast.dto.validacion.LongitudBcryptValida;

/** Cambio de contraseña desde "Mi perfil" (RF11): pide la actual y la confirmación de la nueva. */
@ContrasenasCoinciden
public record CambioContrasenaDto(
		@NotBlank(message = "La contraseña actual no es correcta.") String contrasenaActual,
		@NotNull(message = "La contraseña debe tener al menos 8 caracteres.") @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres.") @LongitudBcryptValida String contrasena,
		@NotBlank(message = "Las contraseñas no coinciden.") String confirmacion) implements ContrasenaConfirmada {

	public static CambioContrasenaDto vacio() {
		return new CambioContrasenaDto("", "", "");
	}
}
