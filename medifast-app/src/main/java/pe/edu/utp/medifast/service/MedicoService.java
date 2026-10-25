package pe.edu.utp.medifast.service;

/**
 * Servicio encargado de operaciones relacionadas con la identidad del médico
 * autenticado.
 */
public interface MedicoService {

    /**
     * Obtiene el identificador del médico asociado al número de documento
     * utilizado durante la autenticación.
     *
     * @param numeroDocumento número de documento del usuario autenticado
     * @return identificador del médico
     * @throws IllegalStateException si no existe un médico asociado
     */
    Long obtenerIdPorDocumento(String numeroDocumento);
}
