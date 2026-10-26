package pe.edu.utp.medifast.dto.validacion;

import java.nio.charset.StandardCharsets;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Cuenta bytes UTF-8 porque bcrypt limita bytes, no caracteres Unicode. */
public class LongitudBcryptValidaValidator implements ConstraintValidator<LongitudBcryptValida, String> {

	@Override
	public boolean isValid(String contrasena, ConstraintValidatorContext context) {
		return contrasena == null || contrasena.getBytes(StandardCharsets.UTF_8).length <= 72;
	}
}
