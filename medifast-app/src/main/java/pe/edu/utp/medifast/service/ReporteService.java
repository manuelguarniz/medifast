package pe.edu.utp.medifast.service;

import pe.edu.utp.medifast.dto.FiltroReporteDto;
import pe.edu.utp.medifast.dto.ReporteCitasDto;
import pe.edu.utp.medifast.dto.ResumenPagosDto;

/**
 * Consultas de los reportes del médico (RF09): citas por rango y estado (P9) y pagos validados por período (P10).
 * La exportación a Excel la hace {@link pe.edu.utp.medifast.util.ExportadorReporte}.
 */
public interface ReporteService {

    /**
     * Genera el reporte de citas del médico autenticado dentro del rango y,
     * opcionalmente, del estado solicitado.
     *
     * @param idMedico identificador del médico autenticado
     * @param filtro rango de fechas y estado opcional
     * @return filas de citas y totales para los cinco estados
     */
    ReporteCitasDto citas(Long idMedico, FiltroReporteDto filtro);

    /**
     * Pagos validados de un médico cuya fecha de validación cae dentro del rango indicado.
     *
     * @param idMedico identificador del médico autenticado
     * @param filtro rango de fechas del reporte (por fecha de validación del pago)
     * @return un pago por fila con la cantidad, el total y el ticket promedio
     */
    ResumenPagosDto pagos(Long idMedico, FiltroReporteDto filtro);
}
