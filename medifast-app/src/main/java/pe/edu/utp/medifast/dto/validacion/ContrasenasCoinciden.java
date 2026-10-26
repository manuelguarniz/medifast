package pe.edu.utp.medifast.dto.validacion;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** Verifica que la contraseña y su confirmación sean iguales. */
@Documented
@Constraint(validatedBy = ContrasenasCoincidenValidator.class)
@Target(TYPE)
@Retention(RUNTIME)
public @interface ContrasenasCoinciden {

	String message() default "Las contraseñas no coinciden.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
