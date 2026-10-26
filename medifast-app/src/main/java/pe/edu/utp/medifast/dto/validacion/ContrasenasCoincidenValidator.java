package pe.edu.utp.medifast.dto.validacion;

import java.util.Objects;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import pe.edu.utp.medifast.dto.RegistroDto;

/** Asocia el error de confirmación con el campo que debe corregir la persona. */
public class ContrasenasCoincidenValidator implements ConstraintValidator<ContrasenasCoinciden, RegistroDto> {

	@Override
	public boolean isValid(RegistroDto registro, ConstraintValidatorContext context) {
		if (registro == null || registro.contrasena() == null || registro.confirmacion() == null
				|| registro.contrasena().isBlank() || registro.confirmacion().isBlank()
				|| Objects.equals(registro.contrasena(), registro.confirmacion())) {
			return true;
		}

		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate("Las contraseñas no coinciden.")
				.addPropertyNode("confirmacion")
				.addConstraintViolation();
		return false;
	}
}
