package pe.edu.utp.medifast.service;

import pe.edu.utp.medifast.dto.FiltroReporteDto;
import pe.edu.utp.medifast.dto.ResumenPagosDto;

public interface ReporteService {
     /**
     * Genera el resumen de pagos validados de un médico dentro del rango indicado.
     * El último día del filtro se incluye por completo.
     *
     * @param idMedico identificador del médico autenticado
     * @param filtro rango de fechas del reporte
     * @return resumen agrupado por día, totales y ticket promedio
     */
    ResumenPagosDto pagos(Long idMedico, FiltroReporteDto filtro);
}
