package pe.edu.utp.medifast.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import pe.edu.utp.medifast.dto.FiltroReporteDto;
import pe.edu.utp.medifast.dto.ReporteCitasDto;
import pe.edu.utp.medifast.dto.ResumenPagosDto;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;
import pe.edu.utp.medifast.service.MedicoService;
import pe.edu.utp.medifast.service.ReporteService;
import pe.edu.utp.medifast.util.ExportadorReporte;
import pe.edu.utp.medifast.util.Reporte;

@Controller
public class ReporteController {

    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReporteService reporteService;
    private final MedicoService medicoService;
    private final ExportadorReporte exportadorReporte;

    public ReporteController(
            ReporteService reporteService,
            MedicoService medicoService,
            ExportadorReporte exportadorReporte) {
        this.reporteService = reporteService;
        this.medicoService = medicoService;
        this.exportadorReporte = exportadorReporte;
    }

    @GetMapping("/medico/reportes/citas")
    public String reporteCitas(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate hasta,
            @RequestParam(required = false)
            EstadoCitaEnum estado,
            Authentication authentication,
            Model model) {

        LocalDate hoy = LocalDate.now();
        LocalDate inicioSemana = hoy.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate finSemana = inicioSemana.plusDays(6);

        LocalDate fechaDesde = desde != null
                ? desde
                : hasta != null ? hasta : inicioSemana;

        LocalDate fechaHasta = hasta != null
                ? hasta
                : desde != null ? desde : finSemana;

        FiltroReporteDto filtro;

        try {
            filtro = new FiltroReporteDto(
                    fechaDesde,
                    fechaHasta,
                    estado);
        } catch (IllegalArgumentException e) {
            ReporteCitasDto reporteVacio = new ReporteCitasDto(List.of());
            cargarModeloCitas(
                    model,
                    reporteVacio,
                    fechaDesde,
                    fechaHasta,
                    estado,
                    hoy,
                    inicioSemana,
                    finSemana);
            model.addAttribute("error", e.getMessage());
            return "medico/reporte-citas";
        }

        Long idMedico = medicoService.obtenerIdPorDocumento(
                authentication.getName());

        ReporteCitasDto reporte = reporteService.citas(idMedico, filtro);

        cargarModeloCitas(
                model,
                reporte,
                fechaDesde,
                fechaHasta,
                estado,
                hoy,
                inicioSemana,
                finSemana);

        return "medico/reporte-citas";
    }

    @GetMapping("/medico/reportes/citas.xlsx")
    public ResponseEntity<byte[]> exportarCitas(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate hasta,
            @RequestParam(required = false)
            EstadoCitaEnum estado,
            Authentication authentication) {

        LocalDate hoy = LocalDate.now();
        LocalDate inicioSemana = hoy.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate finSemana = inicioSemana.plusDays(6);

        LocalDate fechaDesde = desde != null
                ? desde
                : hasta != null ? hasta : inicioSemana;

        LocalDate fechaHasta = hasta != null
                ? hasta
                : desde != null ? desde : finSemana;

        FiltroReporteDto filtro = new FiltroReporteDto(
                fechaDesde,
                fechaHasta,
                estado);

        Long idMedico = medicoService.obtenerIdPorDocumento(
                authentication.getName());

        ReporteCitasDto resultado = reporteService.citas(idMedico, filtro);
        Reporte reporte = crearReporteCitas(resultado, filtro);
        byte[] archivo = exportadorReporte.exportar(reporte);

        String nombreArchivo = "reporte-citas-"
                + fechaDesde
                + "-"
                + fechaHasta
                + ".xlsx";

        return respuestaExcel(archivo, nombreArchivo);
    }

    @GetMapping("/medico/reportes/pagos")
    public String reportePagos(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate hasta,
            Authentication authentication,
            Model model) {

        YearMonth mesActual = YearMonth.now();

        LocalDate fechaDesde = desde != null
                ? desde
                : mesActual.atDay(1);

        LocalDate fechaHasta = hasta != null
                ? hasta
                : mesActual.atEndOfMonth();

        Long idMedico = medicoService.obtenerIdPorDocumento(
                authentication.getName());

        ResumenPagosDto resumen = reporteService.pagos(
                idMedico,
                new FiltroReporteDto(fechaDesde, fechaHasta));

        YearMonth mesAnterior = mesActual.minusMonths(1);

        model.addAttribute("resumen", resumen);
        model.addAttribute("desde", fechaDesde);
        model.addAttribute("hasta", fechaHasta);
        model.addAttribute("inicioMesActual", mesActual.atDay(1));
        model.addAttribute("finMesActual", mesActual.atEndOfMonth());
        model.addAttribute("inicioMesAnterior", mesAnterior.atDay(1));
        model.addAttribute("finMesAnterior", mesAnterior.atEndOfMonth());

        return "medico/reporte-pagos";
    }

    @GetMapping("/medico/reportes/pagos.xlsx")
    public ResponseEntity<byte[]> exportarPagos(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate hasta,
            Authentication authentication) {

        YearMonth mesActual = YearMonth.now();

        LocalDate fechaDesde = desde != null
                ? desde
                : mesActual.atDay(1);

        LocalDate fechaHasta = hasta != null
                ? hasta
                : mesActual.atEndOfMonth();

        Long idMedico = medicoService.obtenerIdPorDocumento(
                authentication.getName());

        ResumenPagosDto resumen = reporteService.pagos(
                idMedico,
                new FiltroReporteDto(fechaDesde, fechaHasta));

        Reporte reporte = crearReportePagos(
                resumen,
                fechaDesde,
                fechaHasta);

        byte[] archivo = exportadorReporte.exportar(reporte);

        return respuestaExcel(archivo, "reporte-pagos.xlsx");
    }


    private void cargarModeloCitas(
            Model model,
            ReporteCitasDto reporte,
            LocalDate desde,
            LocalDate hasta,
            EstadoCitaEnum estado,
            LocalDate hoy,
            LocalDate inicioSemana,
            LocalDate finSemana) {

        model.addAttribute("reporte", reporte);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        model.addAttribute("estado", estado);
        model.addAttribute("hoy", hoy);
        model.addAttribute("inicioSemana", inicioSemana);
        model.addAttribute("finSemana", finSemana);
        model.addAttribute(
                "pendientesPago",
                reporte.total(EstadoCitaEnum.PENDIENTE_PAGO));
        model.addAttribute(
                "programadas",
                reporte.total(EstadoCitaEnum.PROGRAMADA));
        model.addAttribute(
                "atendidas",
                reporte.total(EstadoCitaEnum.ATENDIDA));
        model.addAttribute(
                "noAsistieron",
                reporte.total(EstadoCitaEnum.NO_ASISTIO));
        model.addAttribute(
                "canceladas",
                reporte.total(EstadoCitaEnum.CANCELADA));
    }

    private Reporte crearReporteCitas(
            ReporteCitasDto resultado,
            FiltroReporteDto filtro) {

        List<List<Object>> filas = resultado.filas().stream()
                .map(cita -> List.<Object>of(
                        cita.getIdCita(),
                        cita.getFecha(),
                        cita.getHoraInicio(),
                        cita.getPaciente(),
                        cita.getTipoCita(),
                        cita.getEstado(),
                        cita.getMonto() == null ? "" : cita.getMonto()))
                .toList();

        String estado = filtro.estado() == null
                ? "Todos"
                : filtro.estado().name();

        return new Reporte(
                "Reporte de citas",
                "Desde: " + filtro.desde()
                        + " | Hasta: " + filtro.hasta()
                        + " | Estado: " + estado,
                List.of(
                        "N.º cita",
                        "Fecha",
                        "Hora",
                        "Paciente",
                        "Tipo",
                        "Estado",
                        "Monto"),
                filas,
                List.of(
                        "Totales",
                        "Pendiente: " + resultado.total(EstadoCitaEnum.PENDIENTE_PAGO),
                        "Programada: " + resultado.total(EstadoCitaEnum.PROGRAMADA),
                        "Atendida: " + resultado.total(EstadoCitaEnum.ATENDIDA),
                        "No asistió: " + resultado.total(EstadoCitaEnum.NO_ASISTIO),
                        "Cancelada: " + resultado.total(EstadoCitaEnum.CANCELADA),
                        "Filas: " + resultado.filas().size()));
    }

    private Reporte crearReportePagos(
            ResumenPagosDto resumen,
            LocalDate desde,
            LocalDate hasta) {

        List<List<Object>> filas = resumen.getDias().stream()
                .map(dia -> List.<Object>of(
                        dia.getFecha(),
                        dia.getCantidad(),
                        dia.getTotal()))
                .toList();

        return new Reporte(
                "Reporte de pagos validados",
                "Desde: " + desde + " | Hasta: " + hasta,
                List.of("Fecha", "Cantidad", "Total S/"),
                filas,
                List.of(
                        "Totales",
                        resumen.getCantidadTotal(),
                        resumen.getMontoTotal()));
    }

    private ResponseEntity<byte[]> respuestaExcel(
            byte[] archivo,
            String nombreArchivo) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(nombreArchivo)
                        .build());

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(XLSX)
                .body(archivo);
    }
}
