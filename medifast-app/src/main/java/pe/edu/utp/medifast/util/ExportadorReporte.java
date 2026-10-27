package pe.edu.utp.medifast.util;

/**
 * Contrato genérico para exportar reportes de MEDIFAST.
 */
public interface ExportadorReporte {

    /**
     * Exporta un reporte a un arreglo de bytes.
     *
     * @param reporte estructura genérica del reporte
     * @return contenido del archivo exportado
     */
    byte[] exportar(Reporte reporte);
}
