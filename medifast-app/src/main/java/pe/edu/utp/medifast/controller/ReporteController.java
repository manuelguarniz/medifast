package pe.edu.utp.medifast.controller;

import java.net.URI;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.base.Enums;
import com.google.common.collect.ImmutableMap;

import pe.edu.utp.medifast.dto.FiltroReporteDto;
import pe.edu.utp.medifast.dto.PagoValidadoDto;
import pe.edu.utp.medifast.dto.ReporteCitasDto;
import pe.edu.utp.medifast.dto.ResumenPagosDto;
import pe.edu.utp.medifast.model.Medico;
import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;
import pe.edu.utp.medifast.security.UsuarioActual;
import pe.edu.utp.medifast.service.ReporteService;
import pe.edu.utp.medifast.util.EtiquetasEstado;
import pe.edu.utp.medifast.util.ExportadorReporte;
import pe.edu.utp.medifast.util.Reporte;

/**
 * P9 — Reporte de citas y P10 — Reporte de pagos (RF09): filtros por rango de fechas con atajos, indicadores,
 * tabla y exportación a .xlsx con los mismos filtros.
 */
@Controller
@RequestMapping("/medico/reportes")
public class ReporteController {

    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private static final String VISTA_CITAS = "medico/reporte-citas";
    private static final String VISTA_PAGOS = "medico/reporte-pagos";
    private static final String TODOS_LOS_ESTADOS = "Todos los estados";

    /** Opciones del filtro "Estado": los 5 estados de la cita con sus etiquetas de README §5.2. */
    private static final Map<String, String> ESTADOS = Arrays.stream(EstadoCitaEnum.values())
            .collect(ImmutableMap.toImmutableMap(EstadoCitaEnum::name, EtiquetasEstado::cita));

    private final ReporteService reporteService;
    private final ExportadorReporte exportadorReporte;
    private final UsuarioActual usuarioActual;
    private final Clock clock;

    public ReporteController(ReporteService reporteService, ExportadorReporte exportadorReporte,
            UsuarioActual usuarioActual, Clock clock) {
        this.reporteService = reporteService;
        this.exportadorReporte = exportadorReporte;
        this.usuarioActual = usuarioActual;
        this.clock = clock;
    }

    /**
     * P9. Por defecto, "Esta semana" (lunes a sábado) y todos los estados. Si las fechas no son válidas no se
     * consulta y el error se muestra bajo el campo.
     */
    @GetMapping("/citas")
    public String reporteCitas(@RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta, @RequestParam(required = false) String estado,
            Authentication authentication, Model model) {
        Medico medico = usuarioActual.medico(authentication);
        List<Atajo> atajos = atajos(LocalDate.now(clock), DayOfWeek.SATURDAY);
        Fechas fechas = Fechas.leer(desde, hasta, atajos.get(1));
        EstadoCitaEnum filtroEstado = leerEstado(estado);
        cargarComunes(model, medico, "citas", fechas, atajos);
        model.addAttribute("estado", filtroEstado == null ? null : filtroEstado.name());
        model.addAttribute("estados", ESTADOS);
        if (!fechas.valido()) {
            return VISTA_CITAS;
        }

        ReporteCitasDto reporte = reporteService.citas(medico.getId(),
                new FiltroReporteDto(fechas.desde(), fechas.hasta(), filtroEstado));
        long total = reporte.filas().size();
        model.addAttribute("filas", reporte.filas().stream().map(ReporteController::filaCita).toList());
        model.addAttribute("total", total);
        model.addAttribute("atendidas", reporte.total(EstadoCitaEnum.ATENDIDA));
        model.addAttribute("canceladas", reporte.total(EstadoCitaEnum.CANCELADA));
        model.addAttribute("noAsistieron", reporte.total(EstadoCitaEnum.NO_ASISTIO));
        model.addAttribute("pctAtendidas", porcentaje(reporte.total(EstadoCitaEnum.ATENDIDA), total));
        model.addAttribute("pctCanceladas", porcentaje(reporte.total(EstadoCitaEnum.CANCELADA), total));
        model.addAttribute("pctNoAsistieron", porcentaje(reporte.total(EstadoCitaEnum.NO_ASISTIO), total));
        model.addAttribute("porEstado", Arrays.stream(EstadoCitaEnum.values())
                .map(e -> new TotalEstado(EtiquetasEstado.cita(e), EtiquetasEstado.claseCita(e), reporte.total(e)))
                .toList());
        model.addAttribute("contador", total == 1 ? "1 cita" : total + " citas");
        return VISTA_CITAS;
    }

    /** Descarga P9 en .xlsx con los mismos filtros; con fechas inválidas vuelve a la pantalla con el error. */
    @GetMapping("/citas.xlsx")
    public ResponseEntity<byte[]> exportarCitas(@RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta, @RequestParam(required = false) String estado,
            Authentication authentication) {
        Fechas fechas = Fechas.leer(desde, hasta, atajos(LocalDate.now(clock), DayOfWeek.SATURDAY).get(1));
        if (!fechas.valido()) {
            return volverA("/medico/reportes/citas", desde, hasta, estado);
        }
        EstadoCitaEnum filtroEstado = leerEstado(estado);
        FiltroReporteDto filtro = new FiltroReporteDto(fechas.desde(), fechas.hasta(), filtroEstado);
        ReporteCitasDto reporte = reporteService.citas(usuarioActual.idMedico(authentication), filtro);
        return respuestaExcel(exportadorReporte.exportar(reporteCitas(reporte, filtro)),
                "reporte-citas-" + fechas.desde() + "-" + fechas.hasta() + ".xlsx");
    }

    /**
     * P10. Por defecto, "Esta semana"; el período filtra por la fecha de validación del pago, que puede caer
     * cualquier día, por eso la semana va de lunes a domingo.
     */
    @GetMapping("/pagos")
    public String reportePagos(@RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta, Authentication authentication, Model model) {
        Medico medico = usuarioActual.medico(authentication);
        List<Atajo> atajos = atajos(LocalDate.now(clock), DayOfWeek.SUNDAY);
        Fechas fechas = Fechas.leer(desde, hasta, atajos.get(1));
        cargarComunes(model, medico, "pagos", fechas, atajos);
        if (!fechas.valido()) {
            return VISTA_PAGOS;
        }

        ResumenPagosDto resumen = reporteService.pagos(medico.getId(),
                new FiltroReporteDto(fechas.desde(), fechas.hasta()));
        model.addAttribute("filas", resumen.pagos().stream().map(ReporteController::filaPago).toList());
        model.addAttribute("cantidad", resumen.cantidad());
        model.addAttribute("totalSoles", FormatoVista.soles(resumen.total()));
        model.addAttribute("ticketPromedio", FormatoVista.soles(resumen.ticketPromedio()));
        model.addAttribute("contador", resumen.cantidad() == 1 ? "1 pago" : resumen.cantidad() + " pagos");
        return VISTA_PAGOS;
    }

    /** Descarga P10 en .xlsx: filas, fila de total y los 3 indicadores. */
    @GetMapping("/pagos.xlsx")
    public ResponseEntity<byte[]> exportarPagos(@RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta, Authentication authentication) {
        Fechas fechas = Fechas.leer(desde, hasta, atajos(LocalDate.now(clock), DayOfWeek.SUNDAY).get(1));
        if (!fechas.valido()) {
            return volverA("/medico/reportes/pagos", desde, hasta, null);
        }
        ResumenPagosDto resumen = reporteService.pagos(usuarioActual.idMedico(authentication),
                new FiltroReporteDto(fechas.desde(), fechas.hasta()));
        return respuestaExcel(exportadorReporte.exportar(reportePagos(resumen, fechas)),
                "reporte-pagos-" + fechas.desde() + "-" + fechas.hasta() + ".xlsx");
    }

    private static void cargarComunes(Model model, Medico medico, String activo, Fechas fechas, List<Atajo> atajos) {
        model.addAttribute("nombreMedico", FormatoVista.nombreMedico(medico));
        model.addAttribute("activo", activo);
        model.addAttribute("fechas", fechas);
        model.addAttribute("atajos", atajos);
        model.addAttribute("atajoActivo", fechas.valido() ? atajos.stream()
                .filter(atajo -> atajo.desde().equals(fechas.desde()) && atajo.hasta().equals(fechas.hasta()))
                .map(Atajo::nombre)
                .findFirst()
                .orElse(null) : null);
        model.addAttribute("notaRango", fechas.valido() ? FormatoVista.rango(fechas.desde(), fechas.hasta()) : null);
    }

    /** "Hoy", "Esta semana" (lunes al {@code finSemana}) y "Este mes"; solo rellenan Desde y Hasta. */
    private static List<Atajo> atajos(LocalDate hoy, DayOfWeek finSemana) {
        LocalDate lunes = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        YearMonth mes = YearMonth.from(hoy);
        return List.of(
                new Atajo("Hoy", hoy, hoy),
                new Atajo("Esta semana", lunes, lunes.plusDays(finSemana.getValue() - 1L)),
                new Atajo("Este mes", mes.atDay(1), mes.atEndOfMonth()));
    }

    private static EstadoCitaEnum leerEstado(String estado) {
        return Enums.getIfPresent(EstadoCitaEnum.class, StringUtils.defaultString(estado)).orNull();
    }

    /** "40 % del total"; sin citas, 0 %. */
    private static String porcentaje(long parte, long total) {
        return (total == 0 ? 0 : Math.round(parte * 100.0 / total)) + " % del total";
    }

    private static FilaCita filaCita(VistaCita cita) {
        return new FilaCita(
                cita.getIdCita(),
                FormatoVista.numeroCita(cita.getIdCita()),
                FormatoVista.fechaCorta(cita.getFecha()),
                FormatoVista.HORA.format(cita.getHoraInicio()),
                cita.getPaciente(),
                FormatoVista.documento(cita.getTipoDocumento(), cita.getNumeroDocumento()),
                EtiquetasEstado.tipo(cita.getTipoCita()),
                EtiquetasEstado.cita(cita.getEstado()),
                EtiquetasEstado.claseCita(cita.getEstado()),
                cita.getEstadoPago() == null ? null : EtiquetasEstado.pago(cita.getEstadoPago()),
                EtiquetasEstado.clasePago(cita.getEstadoPago()));
    }

    private static FilaPago filaPago(PagoValidadoDto pago) {
        return new FilaPago(
                pago.idCita(),
                FormatoVista.numeroCita(pago.idCita()),
                FormatoVista.fechaHoraCorta(pago.validadoEn()),
                pago.paciente(),
                FormatoVista.documento(pago.tipoDocumento(), pago.numeroDocumento()),
                EtiquetasEstado.tipo(pago.tipoCita()),
                StringUtils.defaultIfBlank(pago.nroOperacion(), null),
                FormatoVista.soles(pago.monto()));
    }

    /**
     * Mismas columnas que la tabla de P9, con las etiquetas de §5.2 y el documento abreviado (RNF07); el resumen
     * trae el total y cada uno de los 5 estados (RF09).
     */
    private static Reporte reporteCitas(ReporteCitasDto resultado, FiltroReporteDto filtro) {
        List<List<Object>> filas = resultado.filas().stream()
                .map(cita -> List.<Object>of(
                        FormatoVista.numeroCita(cita.getIdCita()),
                        cita.getFecha(),
                        cita.getHoraInicio(),
                        cita.getPaciente(),
                        FormatoVista.documento(cita.getTipoDocumento(), cita.getNumeroDocumento()),
                        EtiquetasEstado.tipo(cita.getTipoCita()),
                        EtiquetasEstado.cita(cita.getEstado()),
                        EtiquetasEstado.pago(cita.getEstadoPago())))
                .toList();
        long total = resultado.filas().size();
        List<List<Object>> resumen = new ArrayList<>();
        resumen.add(List.of("Total de citas", total));
        for (EstadoCitaEnum e : EstadoCitaEnum.values()) {
            resumen.add(List.of(EtiquetasEstado.cita(e), resultado.total(e), porcentaje(resultado.total(e), total)));
        }
        String estado = filtro.estado() == null ? TODOS_LOS_ESTADOS : EtiquetasEstado.cita(filtro.estado());

        return new Reporte(
                "Reporte de citas",
                filtrosEnTexto(filtro.desde(), filtro.hasta()) + " | Estado: " + estado,
                List.of("N.º", "Fecha", "Hora", "Paciente", "Documento", "Tipo", "Cita", "Pago"),
                filas,
                List.of(total == 1 ? "1 cita" : total + " citas"),
                resumen);
    }

    private static Reporte reportePagos(ResumenPagosDto resumen, Fechas fechas) {
        List<List<Object>> filas = resumen.pagos().stream()
                .map(pago -> List.<Object>of(
                        pago.validadoEn(),
                        FormatoVista.numeroCita(pago.idCita()),
                        pago.paciente(),
                        FormatoVista.documento(pago.tipoDocumento(), pago.numeroDocumento()),
                        EtiquetasEstado.tipo(pago.tipoCita()),
                        StringUtils.defaultIfBlank(pago.nroOperacion(), "—"),
                        pago.monto()))
                .toList();
        List<Object> totales = new ArrayList<>(List.of(
                "Total · " + (resumen.cantidad() == 1 ? "1 pago" : resumen.cantidad() + " pagos"),
                "", "", "", "", ""));
        totales.add(resumen.total());

        return new Reporte(
                "Reporte de pagos verificados",
                filtrosEnTexto(fechas.desde(), fechas.hasta()) + " | Por fecha de validación del pago",
                List.of("Validado el", "N.º de cita", "Paciente", "Documento", "Tipo", "N.º de operación", "Monto"),
                filas,
                totales,
                List.of(
                        List.of("Pagos verificados", resumen.cantidad()),
                        List.of("Total verificado", resumen.total()),
                        List.of("Ticket promedio", resumen.ticketPromedio())));
    }

    private static String filtrosEnTexto(LocalDate desde, LocalDate hasta) {
        return "Desde: " + FormatoVista.FECHA_INPUT.format(desde) + " | Hasta: " + FormatoVista.FECHA_INPUT.format(hasta);
    }

    private static ResponseEntity<byte[]> volverA(String ruta, String desde, String hasta, String estado) {
        URI destino = UriComponentsBuilder.fromPath(ruta)
                .queryParamIfPresent("desde", Optional.ofNullable(desde))
                .queryParamIfPresent("hasta", Optional.ofNullable(hasta))
                .queryParamIfPresent("estado", Optional.ofNullable(estado))
                .build()
                .encode()
                .toUri();
        return ResponseEntity.status(HttpStatus.FOUND).location(destino).build();
    }

    private static ResponseEntity<byte[]> respuestaExcel(byte[] archivo, String nombreArchivo) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nombreArchivo).build().toString())
                .contentType(XLSX)
                .body(archivo);
    }

    /** Atajo de período: rellena Desde y Hasta sin consultar. */
    public record Atajo(String nombre, LocalDate desde, LocalDate hasta) {
    }

    /**
     * Fechas del filtro leídas como texto para validarlas en el servidor. Sin parámetros se usa el período por
     * defecto; un campo vacío o con formato inválido pide la fecha, y "Hasta" anterior a "Desde" marca solo "Hasta"
     * (diseño de P9).
     *
     * @param valorDesde texto que vuelve al input
     * @param valorHasta texto que vuelve al input
     */
    public record Fechas(String valorDesde, String valorHasta, LocalDate desde, LocalDate hasta, String errorDesde,
            String errorHasta) {

        static Fechas leer(String desde, String hasta, Atajo porDefecto) {
            if (desde == null && hasta == null) {
                return new Fechas(porDefecto.desde().toString(), porDefecto.hasta().toString(), porDefecto.desde(),
                        porDefecto.hasta(), null, null);
            }
            LocalDate inicio = parsear(desde);
            LocalDate fin = parsear(hasta);
            String errorDesde = inicio == null ? FiltroReporteDto.FECHA_OBLIGATORIA : null;
            String errorHasta = null;
            if (fin == null) {
                errorHasta = FiltroReporteDto.FECHA_OBLIGATORIA;
            } else if (inicio != null && fin.isBefore(inicio)) {
                errorHasta = FiltroReporteDto.RANGO_INVALIDO;
            }
            return new Fechas(StringUtils.defaultString(desde), StringUtils.defaultString(hasta), inicio, fin,
                    errorDesde, errorHasta);
        }

        private static LocalDate parsear(String fecha) {
            if (StringUtils.isBlank(fecha)) {
                return null;
            }
            try {
                return LocalDate.parse(fecha.strip());
            } catch (DateTimeParseException e) {
                return null;
            }
        }

        public boolean valido() {
            return errorDesde == null && errorHasta == null;
        }
    }

    /** Total de un estado de la cita (RF09: los 5 estados), con la clase del badge. */
    public record TotalEstado(String etiqueta, String clase, long cantidad) {
    }

    /** Fila de P9; {@code etiquetaPago} es {@code null} mientras la cita no tiene pago (se muestra "—"). */
    public record FilaCita(Long id, String numero, String fecha, String hora, String paciente, String documento,
            String tipo, String etiquetaCita, String claseCita, String etiquetaPago, String clasePago) {
    }

    /** Fila de P10; {@code operacion} es {@code null} si el paciente no ingresó el n.º (RN-11). */
    public record FilaPago(Long idCita, String numero, String validadoEl, String paciente, String documento,
            String tipo, String operacion, String monto) {
    }
}
