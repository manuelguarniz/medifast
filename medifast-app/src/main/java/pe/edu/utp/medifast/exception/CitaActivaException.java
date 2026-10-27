package pe.edu.utp.medifast.exception;

import pe.edu.utp.medifast.model.VistaCita;

public class CitaActivaException extends RuntimeException {
    private final VistaCita citaActiva;

    public CitaActivaException(String mensaje, VistaCita citaActiva) {
        super(mensaje);
        this.citaActiva = citaActiva;
    }

    public VistaCita getCitaActiva() {
        return citaActiva;
    }
}
