package pe.edu.utp.medifast.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import pe.edu.utp.medifast.dao.PagoDao;
import pe.edu.utp.medifast.dao.VistaCitaDao;
import pe.edu.utp.medifast.dto.FiltroReporteDto;
import pe.edu.utp.medifast.dto.ReporteCitasDto;
import pe.edu.utp.medifast.dto.ResumenPagosDto;
import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.service.ReporteService;

/** Implementación de los reportes sobre la vista {@code v_cita} y los pagos validados. */
@Service
public class ReporteServiceImpl implements ReporteService {

    private final PagoDao pagoDao;
    private final VistaCitaDao vistaCitaDao;

    public ReporteServiceImpl(PagoDao pagoDao, VistaCitaDao vistaCitaDao) {
        this.pagoDao = pagoDao;
        this.vistaCitaDao = vistaCitaDao;
    }

    @Override
    public ReporteCitasDto citas(Long idMedico, FiltroReporteDto filtro) {
        List<VistaCita> filas = vistaCitaDao.reporteCitas(
                idMedico,
                filtro.desde(),
                filtro.hasta(),
                filtro.estado());

        return new ReporteCitasDto(filas);
    }

    @Override
    public ResumenPagosDto pagos(Long idMedico, FiltroReporteDto filtro) {
        LocalDateTime desde = filtro.desde().atStartOfDay();
        LocalDateTime hastaExclusivo = filtro.hasta().plusDays(1).atStartOfDay();
        return new ResumenPagosDto(pagoDao.pagosValidados(idMedico, desde, hastaExclusivo));
    }
}
