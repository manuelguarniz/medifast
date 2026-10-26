package pe.edu.utp.medifast.exception;

import pe.edu.utp.medifast.model.Cita;

/** El paciente ya tiene una cita que impide reservar otra (RN-09). */
public class CitaActivaException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final Cita citaActiva;

	public CitaActivaException(Cita citaActiva) {
		super("Ya tienes una cita activa. Solo puedes tener una a la vez.");
		this.citaActiva = citaActiva;
	}

	public Cita getCitaActiva() {
		return citaActiva;
	}
}
