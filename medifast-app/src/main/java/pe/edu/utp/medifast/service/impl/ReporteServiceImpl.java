package pe.edu.utp.medifast.service.impl;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import pe.edu.utp.medifast.dao.PagoDao;
import pe.edu.utp.medifast.dao.VistaCitaDao;
import pe.edu.utp.medifast.dto.FiltroReporteDto;
import pe.edu.utp.medifast.dto.PagoDiaDto;
import pe.edu.utp.medifast.dto.ReporteCitasDto;
import pe.edu.utp.medifast.dto.ResumenPagosDto;
import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.service.ReporteService;

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

        List<PagoDiaDto> dias = pagoDao.resumenValidados(idMedico, desde, hastaExclusivo)
                .stream()
                .map(this::convertirFila)
                .toList();

        return new ResumenPagosDto(dias);
    }

    private PagoDiaDto convertirFila(Object[] fila) {
        LocalDate fecha;
        Object valorFecha = fila[0];

        if (valorFecha instanceof Date fechaSql) {
            fecha = fechaSql.toLocalDate();
        } else if (valorFecha instanceof LocalDate fechaLocal) {
            fecha = fechaLocal;
        } else {
            fecha = LocalDate.parse(valorFecha.toString());
        }

        long cantidad = ((Number) fila[1]).longValue();
        BigDecimal total = fila[2] instanceof BigDecimal decimal
                ? decimal
                : new BigDecimal(fila[2].toString());

        return new PagoDiaDto(fecha, cantidad, total);
    }
}
