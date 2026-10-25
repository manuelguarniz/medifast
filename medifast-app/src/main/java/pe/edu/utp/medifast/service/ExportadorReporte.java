package pe.edu.utp.medifast.service;

import java.time.LocalDate;

import pe.edu.utp.medifast.dto.ResumenPagosDto;

/**
 * Contrato para la exportación de reportes de MEDIFAST.
 *
 * Permite que la capa web dependa de una abstracción y no directamente de una
 * implementación específica como Apache POI.
 */
public interface ExportadorReporte {

    /**
     * Exporta el reporte de pagos del período indicado.
     *
     * @param resumen información calculada del reporte
     * @param desde fecha inicial
     * @param hasta fecha final
     * @return contenido binario del archivo generado
     */
    byte[] exportarPagos(ResumenPagosDto resumen, LocalDate desde, LocalDate hasta);
}
