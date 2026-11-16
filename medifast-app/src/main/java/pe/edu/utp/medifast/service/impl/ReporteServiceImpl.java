package pe.edu.utp.medifast.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.utp.medifast.dao.CitaDao;
import pe.edu.utp.medifast.dao.PagoDao;
import pe.edu.utp.medifast.dao.VistaCitaDao;
import pe.edu.utp.medifast.dto.ConteoEstadoDto;
import pe.edu.utp.medifast.dto.FiltroReporteDto;
import pe.edu.utp.medifast.dto.PagoValidadoDto;
import pe.edu.utp.medifast.dto.ReporteCitasDto;
import pe.edu.utp.medifast.dto.ResumenPagosDto;
import pe.edu.utp.medifast.dto.TotalPagosDto;
import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;
import pe.edu.utp.medifast.service.ReporteService;
import pe.edu.utp.medifast.util.Paginacion;

/** Implementación de los reportes sobre la vista {@code v_cita} y los pagos validados. */
@Service
public class ReporteServiceImpl implements ReporteService {

    private final PagoDao pagoDao;
    private final VistaCitaDao vistaCitaDao;
    private final CitaDao citaDao;

    public ReporteServiceImpl(PagoDao pagoDao, VistaCitaDao vistaCitaDao, CitaDao citaDao) {
        this.pagoDao = pagoDao;
        this.vistaCitaDao = vistaCitaDao;
        this.citaDao = citaDao;
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
    @Transactional(readOnly = true)
    public ReporteCitasDto citas(Long idMedico, FiltroReporteDto filtro, String pagina) {
        Map<EstadoCitaEnum, Long> totales = citaDao
                .contarPorEstado(idMedico, filtro.desde(), filtro.hasta(), filtro.estado()).stream()
                .collect(Collectors.toMap(ConteoEstadoDto::estado, ConteoEstadoDto::cantidad));
        long total = totales.values().stream().mapToLong(Long::longValue).sum();
        Paginacion paginacion = Paginacion.de(pagina, Math.toIntExact(total));
        List<VistaCita> filas = total == 0
                ? List.of()
                : vistaCitaDao.reporteCitas(idMedico, filtro.desde(), filtro.hasta(), filtro.estado(),
                        paginacion.pageable());
        return new ReporteCitasDto(filas, totales, paginacion);
    }

    @Override
    public ResumenPagosDto pagos(Long idMedico, FiltroReporteDto filtro) {
        LocalDateTime desde = filtro.desde().atStartOfDay();
        LocalDateTime hastaExclusivo = filtro.hasta().plusDays(1).atStartOfDay();
        return new ResumenPagosDto(pagoDao.pagosValidados(idMedico, desde, hastaExclusivo));
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenPagosDto pagos(Long idMedico, FiltroReporteDto filtro, String pagina) {
        LocalDateTime desde = filtro.desde().atStartOfDay();
        LocalDateTime hastaExclusivo = filtro.hasta().plusDays(1).atStartOfDay();
        TotalPagosDto totales = pagoDao.totalPagosValidados(idMedico, desde, hastaExclusivo);
        Paginacion paginacion = Paginacion.de(pagina, Math.toIntExact(totales.cantidad()));
        List<PagoValidadoDto> pagos = totales.cantidad() == 0
                ? List.of()
                : pagoDao.pagosValidados(idMedico, desde, hastaExclusivo, paginacion.pageable());
        return new ResumenPagosDto(pagos, totales.cantidad(), totales.total(), paginacion);
    }
}
