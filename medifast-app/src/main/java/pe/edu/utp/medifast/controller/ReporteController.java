package pe.edu.utp.medifast.controller;

import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.core.io.ByteArrayResource;
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

import pe.edu.utp.medifast.dao.MedicoDao;
import pe.edu.utp.medifast.dto.FiltroReporteDto;
import pe.edu.utp.medifast.dto.ResumenPagosDto;
import pe.edu.utp.medifast.model.Medico;
import pe.edu.utp.medifast.service.ExportadorReporte;
import pe.edu.utp.medifast.service.MedicoService;
import pe.edu.utp.medifast.service.ReporteService;
import pe.edu.utp.medifast.util.ExcelExportador;

@Controller 
public class ReporteController {
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

        LocalDate fechaDesde =
                desde != null
                        ? desde
                        : mesActual.atDay(1);

        LocalDate fechaHasta =
                hasta != null
                        ? hasta
                        : mesActual.atEndOfMonth();

        Long idMedico =
                medicoService.obtenerIdPorDocumento(
                        authentication.getName());

        ResumenPagosDto resumen =
                reporteService.pagos(
                        idMedico,
                        new FiltroReporteDto(
                                fechaDesde,
                                fechaHasta));

        YearMonth mesAnterior =
                mesActual.minusMonths(1);

        model.addAttribute(
                "resumen",
                resumen);

        model.addAttribute(
                "desde",
                fechaDesde);

        model.addAttribute(
                "hasta",
                fechaHasta);

        model.addAttribute(
                "inicioMesActual",
                mesActual.atDay(1));

        model.addAttribute(
                "finMesActual",
                mesActual.atEndOfMonth());

        model.addAttribute(
                "inicioMesAnterior",
                mesAnterior.atDay(1));

        model.addAttribute(
                "finMesAnterior",
                mesAnterior.atEndOfMonth());

        return "medico/reporte-pagos";
    }

    @GetMapping("/medico/reportes/pagos.xlsx")
    public ResponseEntity<ByteArrayResource> exportarPagos(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate desde,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate hasta,

            Authentication authentication) {

        YearMonth mesActual = YearMonth.now();

        LocalDate fechaDesde =
                desde != null
                        ? desde
                        : mesActual.atDay(1);

        LocalDate fechaHasta =
                hasta != null
                        ? hasta
                        : mesActual.atEndOfMonth();

        Long idMedico =
                medicoService.obtenerIdPorDocumento(
                        authentication.getName());

        ResumenPagosDto resumen =
                reporteService.pagos(
                        idMedico,
                        new FiltroReporteDto(
                                fechaDesde,
                                fechaHasta));

        byte[] archivo =
                exportadorReporte.exportarPagos(
                        resumen,
                        fechaDesde,
                        fechaHasta);

        HttpHeaders headers =
                new HttpHeaders();

        headers.setContentDisposition(
                ContentDisposition
                        .attachment()
                        .filename("reporte-pagos.xlsx")
                        .build());

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(
                        MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(
                        new ByteArrayResource(
                                archivo));
    }
}
