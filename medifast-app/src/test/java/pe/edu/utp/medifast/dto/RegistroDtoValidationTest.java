package pe.edu.utp.medifast.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import pe.edu.utp.medifast.model.enums.TipoDocumentoEnum;

class RegistroDtoValidationTest {

	private static Validator validator;

	@BeforeAll
	static void crearValidador() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Test
	void aceptaRegistroDniValido() {
		assertThat(validator.validate(dto(TipoDocumentoEnum.DNI, "12345678", "912345678",
				"claveSegura", "claveSegura", LocalDate.now().minusYears(20)))).isEmpty();
	}

	@Test
	void rechazaDniConMenosDeOchoDigitos() {
		assertFieldError(dto(TipoDocumentoEnum.DNI, "1234567", "912345678",
				"claveSegura", "claveSegura", LocalDate.now().minusYears(20)),
				"numeroDocumento", "El DNI debe tener 8 dígitos.");
	}

	@Test
	void rechazaCeFueraDelFormatoPermitido() {
		assertFieldError(dto(TipoDocumentoEnum.CE, "abc", "912345678",
				"claveSegura", "claveSegura", LocalDate.now().minusYears(20)),
				"numeroDocumento", "El CE debe tener entre 9 y 12 caracteres (letras y números).");
	}

	@Test
	void aceptaCeMinusculaQueSeNormalizaAlGuardar() {
		RegistroDto dto = dto(TipoDocumentoEnum.CE, "abc123456", "912345678",
				"claveSegura", "claveSegura", LocalDate.now().minusYears(20));

		assertThat(validator.validate(dto)).isEmpty();
		assertThat(dto.numeroDocumentoNormalizado()).isEqualTo("ABC123456");
	}

	@Test
	void rechazaCelularQueNoEmpiezaConNueve() {
		assertFieldError(dto(TipoDocumentoEnum.DNI, "12345678", "812345678",
				"claveSegura", "claveSegura", LocalDate.now().minusYears(20)),
				"celular", "Ingresa 9 dígitos que empiecen con 9.");
	}

	@Test
	void rechazaFechaDeNacimientoFutura() {
		assertFieldError(dto(TipoDocumentoEnum.DNI, "12345678", "912345678",
				"claveSegura", "claveSegura", LocalDate.now().plusDays(1)),
				"fechaNacimiento", "Ingresa una fecha de nacimiento anterior a hoy.");
	}

	@Test
	void rechazaContrasenaMenorDeOchoCaracteres() {
		assertFieldError(dto(TipoDocumentoEnum.DNI, "12345678", "912345678",
				"clave12", "clave12", LocalDate.now().minusYears(20)),
				"contrasena", "La contraseña debe tener al menos 8 caracteres.");
	}

	@Test
	void rechazaContrasenasDistintasEnElCampoDeConfirmacion() {
		assertFieldError(dto(TipoDocumentoEnum.DNI, "12345678", "912345678",
				"claveSegura", "otraClaveSegura", LocalDate.now().minusYears(20)),
				"confirmacion", "Las contraseñas no coinciden.");
	}

	@Test
	void rechazaContrasenaQueSuperaElLimiteDeBytesDeBcrypt() {
		String contrasena = "á".repeat(37);
		assertFieldError(dto(TipoDocumentoEnum.DNI, "12345678", "912345678",
				contrasena, contrasena, LocalDate.now().minusYears(20)),
				"contrasena", "La contraseña no puede superar 72 bytes.");
	}

	private static RegistroDto dto(TipoDocumentoEnum tipo, String documento, String celular,
			String contrasena, String confirmacion, LocalDate nacimiento) {
		return new RegistroDto(tipo, documento, "Ana", "Pérez", nacimiento, celular, "ana@example.com",
				contrasena, confirmacion);
	}

	private static void assertFieldError(RegistroDto dto, String campo, String mensaje) {
		Set<ConstraintViolation<RegistroDto>> errores = validator.validate(dto);
		assertThat(errores).anySatisfy(error -> {
			assertThat(error.getPropertyPath().toString()).isEqualTo(campo);
			assertThat(error.getMessage()).isEqualTo(mensaje);
		});
	}
}
