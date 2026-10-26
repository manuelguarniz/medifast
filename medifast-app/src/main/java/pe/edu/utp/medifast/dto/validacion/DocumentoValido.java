package pe.edu.utp.medifast.dto.validacion;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** Valida el documento de acuerdo con el tipo seleccionado (RF02). */
@Documented
@Constraint(validatedBy = DocumentoValidoValidator.class)
@Target(TYPE)
@Retention(RUNTIME)
public @interface DocumentoValido {

	String message() default "Ingresa un documento válido.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
