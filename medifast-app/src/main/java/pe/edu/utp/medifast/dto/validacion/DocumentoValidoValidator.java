package pe.edu.utp.medifast.dto.validacion;

import org.apache.commons.lang3.StringUtils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import pe.edu.utp.medifast.dto.RegistroDto;
import pe.edu.utp.medifast.model.enums.TipoDocumentoEnum;

/** Aplica los formatos DNI/CE del esquema y de RF02. */
public class DocumentoValidoValidator implements ConstraintValidator<DocumentoValido, RegistroDto> {

	@Override
	public boolean isValid(RegistroDto registro, ConstraintValidatorContext context) {
		if (registro == null || StringUtils.isBlank(registro.numeroDocumento())
				|| registro.tipoDocumento() == null) {
			return true;
		}

		String documento = StringUtils.deleteWhitespace(registro.numeroDocumento());
		boolean valido = registro.tipoDocumento() == TipoDocumentoEnum.DNI
				? documento.matches("^[0-9]{8}$")
				: documento.matches("^[0-9A-Za-z]{9,12}$");
		if (valido) {
			return true;
		}

		String mensaje = registro.tipoDocumento() == TipoDocumentoEnum.DNI
				? "El DNI debe tener 8 dígitos."
				: "El CE debe tener entre 9 y 12 caracteres (letras y números).";
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(mensaje)
				.addPropertyNode("numeroDocumento")
				.addConstraintViolation();
		return false;
	}
}
