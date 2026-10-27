package pe.edu.utp.medifast.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class PerfilValidationTest {

	private static Validator validator;

	@BeforeAll
	static void crearValidador() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Test
	void aceptaDatosPersonalesValidos() {
		assertThat(validator.validate(perfil("933731536", "andrea.salazar@gmail.com",
				LocalDate.now().minusYears(30)))).isEmpty();
	}

	@Test
	void rechazaCelularQueNoEmpiezaCon9() {
		assertFieldError(perfil("87654321", "andrea.salazar@gmail.com", LocalDate.now().minusYears(30)),
				"celular", "Ingresa 9 dígitos que empiecen con 9.");
	}

	@Test
	void rechazaCorreoInvalido() {
		assertFieldError(perfil("933731536", "andrea.salazar@gmail", LocalDate.now().minusYears(30)),
				"correo", "Ingresa un correo válido.");
	}

	@Test
	void rechazaFechaDeNacimientoNoPasada() {
		assertFieldError(perfil("933731536", "andrea.salazar@gmail.com", LocalDate.now()),
				"fechaNacimiento", "Ingresa una fecha de nacimiento anterior a hoy.");
	}

	@Test
	void aceptaCambioDeContrasenaValido() {
		assertThat(validator.validate(new CambioContrasenaDto("claveActual", "claveNueva1", "claveNueva1")))
				.isEmpty();
	}

	@Test
	void rechazaNuevaContrasenaCorta() {
		assertFieldError(new CambioContrasenaDto("claveActual", "corta", "corta"),
				"contrasena", "La contraseña debe tener al menos 8 caracteres.");
	}

	@Test
	void rechazaConfirmacionDistinta() {
		assertFieldError(new CambioContrasenaDto("claveActual", "claveNueva1", "claveNueva2"),
				"confirmacion", "Las contraseñas no coinciden.");
	}

	@Test
	void exigeLaContrasenaActual() {
		assertFieldError(new CambioContrasenaDto("", "claveNueva1", "claveNueva1"),
				"contrasenaActual", "La contraseña actual no es correcta.");
	}

	@Test
	void muestraUnSoloMensajeSiLaNuevaContrasenaEstaVacia() {
		Set<ConstraintViolation<CambioContrasenaDto>> errores = validator
				.validate(new CambioContrasenaDto("claveActual", "", ""));

		assertThat(errores).filteredOn(error -> error.getPropertyPath().toString().equals("contrasena"))
				.hasSize(1);
	}

	private static PerfilDto perfil(String celular, String correo, LocalDate fechaNacimiento) {
		return new PerfilDto("Andrea Sofía", "Salazar Pérez", fechaNacimiento, celular, correo);
	}

	private static <T> void assertFieldError(T dto, String campo, String mensaje) {
		Set<ConstraintViolation<T>> errores = validator.validate(dto);
		assertThat(errores).anySatisfy(error -> {
			assertThat(error.getPropertyPath().toString()).isEqualTo(campo);
			assertThat(error.getMessage()).isEqualTo(mensaje);
		});
	}
}
