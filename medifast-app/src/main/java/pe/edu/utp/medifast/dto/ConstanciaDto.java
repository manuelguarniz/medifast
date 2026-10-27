package pe.edu.utp.medifast.dto;

import org.springframework.core.io.Resource;

/**
 * Archivo de la constancia de pago listo para enviarse al navegador (RF12).
 *
 * @param archivo contenido guardado en {@code medifast.uploads-dir}
 * @param tipo MIME registrado al subirla: image/jpeg, image/png o application/pdf
 * @param nombre nombre con el que se muestra, sin el UUID interno
 */
public record ConstanciaDto(Resource archivo, String tipo, String nombre) {
}
