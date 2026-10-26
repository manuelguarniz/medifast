package pe.edu.utp.medifast.exception;

public class FueraDeRangoException extends RuntimeException {
    public FueraDeRangoException(String mensaje) {
        super(mensaje);
    }
}