package pe.edu.utp.medifast.dto.validacion;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** Impide contraseñas mayores al límite de 72 bytes de bcrypt. */
@Documented
@Constraint(validatedBy = LongitudBcryptValidaValidator.class)
@Target({ FIELD, PARAMETER, RECORD_COMPONENT })
@Retention(RUNTIME)
public @interface LongitudBcryptValida {

	String message() default "La contraseña no puede superar 72 bytes.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
