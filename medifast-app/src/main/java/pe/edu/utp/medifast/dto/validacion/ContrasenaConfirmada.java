package pe.edu.utp.medifast.dto.validacion;

/** Formulario con una contraseña nueva y su confirmación, validado por {@link ContrasenasCoinciden}. */
public interface ContrasenaConfirmada {

	String contrasena();

	String confirmacion();
}
