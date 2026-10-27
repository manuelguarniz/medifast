package pe.edu.utp.medifast.dto.validacion;

import java.util.Objects;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Asocia el error de confirmación con el campo que debe corregir la persona. */
public class ContrasenasCoincidenValidator implements ConstraintValidator<ContrasenasCoinciden, ContrasenaConfirmada> {

	@Override
	public boolean isValid(ContrasenaConfirmada formulario, ConstraintValidatorContext context) {
		if (formulario == null || formulario.contrasena() == null || formulario.confirmacion() == null
				|| formulario.contrasena().isBlank() || formulario.confirmacion().isBlank()
				|| Objects.equals(formulario.contrasena(), formulario.confirmacion())) {
			return true;
		}

		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate("Las contraseñas no coinciden.")
				.addPropertyNode("confirmacion")
				.addConstraintViolation();
		return false;
	}
}
