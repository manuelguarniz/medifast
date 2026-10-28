package pe.edu.utp.medifast.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import pe.edu.utp.medifast.config.SecurityConfig;
import pe.edu.utp.medifast.dao.UsuarioDao;
import pe.edu.utp.medifast.dto.FiltroReporteDto;
import pe.edu.utp.medifast.dto.PagoValidadoDto;
import pe.edu.utp.medifast.dto.ReporteCitasDto;
import pe.edu.utp.medifast.dto.ResumenPagosDto;
import pe.edu.utp.medifast.model.Medico;
import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;
import pe.edu.utp.medifast.model.enums.EstadoPagoEnum;
import pe.edu.utp.medifast.model.enums.TipoCitaEnum;
import pe.edu.utp.medifast.model.enums.TipoDocumentoEnum;
import pe.edu.utp.medifast.security.EventosAutenticacion;
import pe.edu.utp.medifast.security.IntentosLoginService;
import pe.edu.utp.medifast.security.RedireccionPorRol;
import pe.edu.utp.medifast.security.UsuarioActual;
import pe.edu.utp.medifast.security.UsuarioDetailsService;
import pe.edu.utp.medifast.service.ReporteService;
import pe.edu.utp.medifast.util.Auditoria;
import pe.edu.utp.medifast.util.ExportadorReporte;
import pe.edu.utp.medifast.util.Reporte;

@WebMvcTest(ReporteController.class)
@Import({ SecurityConfig.class, RedireccionPorRol.class, UsuarioDetailsService.class,
        IntentosLoginService.class, EventosAutenticacion.class, ReporteControllerTest.Reloj.class })
class ReporteControllerTest {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final long ID_MEDICO = 1L;
    private static final LocalDate LUNES = LocalDate.of(2026, 11, 9);
    private static final LocalDate SABADO = LocalDate.of(2026, 11, 14);

    /** Hoy es el miércoles 11 de noviembre de 2026, como en el diseño aprobado. */
    @TestConfiguration
    static class Reloj {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-11-11T16:45:00Z"), ZoneId.of("America/Lima"));
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReporteService reporteService;
    @MockitoBean
    private ExportadorReporte exportadorReporte;
    @MockitoBean
    private UsuarioActual usuarioActual;
    @MockitoBean
    private UsuarioDao usuarioDao;
    @MockitoBean
    private Auditoria auditoria;

    @BeforeEach
    void configurar() {
        Medico medico = mock(Medico.class);
        when(medico.getId()).thenReturn(ID_MEDICO);
        when(medico.getNombres()).thenReturn("Moisés");
        when(medico.getApellidos()).thenReturn("Barrantes Cabrera");
        when(usuarioActual.medico(any())).thenReturn(medico);
        when(usuarioActual.idMedico(any())).thenReturn(ID_MEDICO);
    }

    @Test
    void reporteCitasPorDefectoEsEstaSemanaDeLunesASabado() throws Exception {
        ReporteCitasDto reporte = citasDeLaSemana();
        when(reporteService.citas(ID_MEDICO, new FiltroReporteDto(LUNES, SABADO, null))).thenReturn(reporte);

        mockMvc.perform(get("/medico/reportes/citas").with(medico()))
                .andExpect(status().isOk())
                .andExpect(view().name("medico/reporte-citas"))
                .andExpect(model().attribute("atajoActivo", "Esta semana"))
                .andExpect(model().attribute("total", 4L))
                .andExpect(content().string(allOf(
                        containsString("Dr. Moisés Barrantes"),
                        containsString("Del 9 al 14 nov 2026"),
                        containsString("Total de citas"),
                        containsString("50 % del total"),
                        containsString("25 % del total"),
                        containsString("#000118"),
                        containsString("Lun 9 nov 2026"),
                        containsString("09:30"),
                        containsString("Roberto Castro Silva"),
                        containsString("DNI ••••3214"),
                        containsString("Control"),
                        containsString("Atendida"),
                        containsString("Verificado"),
                        containsString("Pago pendiente"),
                        containsString("4 citas"),
                        containsString("Pago pendiente · 1"),
                        containsString("Confirmada · 0"),
                        containsString("Atendida · 2"),
                        containsString("No asistió · 0"),
                        containsString("Cancelada · 1"),
                        containsString("/medico/reportes/citas.xlsx?desde=2026-11-09&amp;hasta=2026-11-14"),
                        not(containsString("70003214")),
                        not(containsString("ATENDIDA</")))));
    }

    @Test
    void reporteCitasAplicaRangoYEstado() throws Exception {
        when(reporteService.citas(any(), any())).thenReturn(new ReporteCitasDto(List.of()));

        mockMvc.perform(get("/medico/reportes/citas").param("desde", "2026-11-01").param("hasta", "2026-11-30")
                        .param("estado", "CANCELADA").with(medico()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("atajoActivo", "Este mes"))
                .andExpect(content().string(allOf(
                        containsString("<option value=\"CANCELADA\" selected=\"selected\">Cancelada</option>"),
                        containsString("No hay citas en este período"),
                        containsString("reporte-exportar-deshabilitado"))));

        verify(reporteService).citas(ID_MEDICO,
                new FiltroReporteDto(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), EstadoCitaEnum.CANCELADA));
    }

    @Test
    void hastaAnteriorADesdeNoConsultaYMuestraElErrorBajoHasta() throws Exception {
        mockMvc.perform(get("/medico/reportes/citas").param("desde", "2026-11-14").param("hasta", "2026-11-09")
                        .with(medico()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("La fecha final debe ser igual o posterior a la inicial."),
                        not(containsString("Total de citas")))));

        verify(reporteService, never()).citas(any(), any());
    }

    @Test
    void fechaVaciaPideIngresarla() throws Exception {
        mockMvc.perform(get("/medico/reportes/citas").param("desde", "").param("hasta", "2026-11-09")
                        .with(medico()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ingresa la fecha.")));

        verifyNoInteractions(reporteService);
    }

    @Test
    void exportarCitasUsaEtiquetasDocumentoParcialYResumen() throws Exception {
        ReporteCitasDto citas = citasDeLaSemana();
        when(reporteService.citas(ID_MEDICO, new FiltroReporteDto(LUNES, SABADO, null))).thenReturn(citas);
        when(exportadorReporte.exportar(any())).thenReturn(new byte[] { 1, 2, 3 });

        mockMvc.perform(get("/medico/reportes/citas.xlsx").param("desde", "2026-11-09").param("hasta", "2026-11-14")
                        .with(medico()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(XLSX))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"reporte-citas-2026-11-09-2026-11-14.xlsx\""))
                .andExpect(content().bytes(new byte[] { 1, 2, 3 }));

        Reporte reporte = reporteExportado();
        assertThat(reporte.encabezados())
                .containsExactly("N.º", "Fecha", "Hora", "Paciente", "Documento", "Tipo", "Cita", "Pago");
        assertThat(reporte.filas().get(0))
                .containsExactly("#000118", LUNES, LocalTime.of(9, 30), "Roberto Castro Silva", "DNI ••••3214",
                        "Control", "Atendida", "Verificado");
        assertThat(reporte.filas().get(3)).endsWith("Pago pendiente", "—");
        assertThat(reporte.filtros()).isEqualTo("Desde: 09/11/2026 | Hasta: 14/11/2026 | Estado: Todos los estados");
        assertThat(reporte.resumen()).containsExactly(
                List.of("Total de citas", 4L),
                List.of("Pago pendiente", 1L, "25 % del total"),
                List.of("Confirmada", 0L, "0 % del total"),
                List.of("Atendida", 2L, "50 % del total"),
                List.of("No asistió", 0L, "0 % del total"),
                List.of("Cancelada", 1L, "25 % del total"));
    }

    @Test
    void exportarConRangoInvalidoVuelveALaPantalla() throws Exception {
        mockMvc.perform(get("/medico/reportes/citas.xlsx").param("desde", "2026-11-14").param("hasta", "2026-11-09")
                        .with(medico()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/medico/reportes/citas?desde=2026-11-14&hasta=2026-11-09"));

        verifyNoInteractions(exportadorReporte);
    }

    @Test
    void reportePagosMuestraUnaFilaPorPagoConTotalEIndicadores() throws Exception {
        LocalDate domingo = LocalDate.of(2026, 11, 15);
        when(reporteService.pagos(ID_MEDICO, new FiltroReporteDto(LUNES, domingo))).thenReturn(pagosDeLaSemana());

        mockMvc.perform(get("/medico/reportes/pagos").with(medico()))
                .andExpect(status().isOk())
                .andExpect(view().name("medico/reporte-pagos"))
                .andExpect(model().attribute("atajoActivo", "Esta semana"))
                .andExpect(content().string(allOf(
                        containsString("Filtra por la fecha de validación del pago."),
                        containsString("Pagos verificados"),
                        containsString("S/ 230.00"),
                        containsString("S/ 115.00"),
                        containsString("Lun 9 nov 2026, 08:40"),
                        containsString("#000118"),
                        containsString("25790114"),
                        containsString("Total · 2 pagos"),
                        containsString("Op. —"),
                        not(containsString("name=\"estado\"")))));
    }

    @Test
    void reportePagosSinResultadosDeshabilitaExportar() throws Exception {
        when(reporteService.pagos(any(), any())).thenReturn(new ResumenPagosDto(List.of()));

        mockMvc.perform(get("/medico/reportes/pagos").param("desde", "2026-11-15").param("hasta", "2026-11-15")
                        .with(medico()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("No hay pagos verificados en este período"),
                        containsString("15 nov 2026"),
                        containsString("S/ 0.00"),
                        containsString("reporte-exportar-deshabilitado"))));
    }

    @Test
    void exportarPagosIncluyeFilaDeTotalEIndicadores() throws Exception {
        when(reporteService.pagos(ID_MEDICO, new FiltroReporteDto(LUNES, SABADO))).thenReturn(pagosDeLaSemana());
        when(exportadorReporte.exportar(any())).thenReturn(new byte[] { 9 });

        mockMvc.perform(get("/medico/reportes/pagos.xlsx").param("desde", "2026-11-09").param("hasta", "2026-11-14")
                        .with(medico()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"reporte-pagos-2026-11-09-2026-11-14.xlsx\""));

        Reporte reporte = reporteExportado();
        assertThat(reporte.filas().get(1)).contains("—");
        assertThat(reporte.totales()).startsWith("Total · 2 pagos").endsWith(new BigDecimal("230.00"));
        assertThat(reporte.resumen()).containsExactly(
                List.of("Pagos verificados", 2L),
                List.of("Total verificado", new BigDecimal("230.00")),
                List.of("Ticket promedio", new BigDecimal("115.00")));
    }

    @Test
    void reporteCitasPaginaLaTablaYConservaLosFiltros() throws Exception {
        ReporteCitasDto reporte = citasAtendidas(23);
        when(reporteService.citas(ID_MEDICO, new FiltroReporteDto(LUNES, SABADO, EstadoCitaEnum.ATENDIDA)))
                .thenReturn(reporte);

        mockMvc.perform(get("/medico/reportes/citas").param("desde", "2026-11-09").param("hasta", "2026-11-14")
                        .param("estado", "ATENDIDA").param("pagina", "2").with(medico()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("total", 23L))
                .andExpect(model().attribute("contador", "Mostrando 11–20 de 23 citas"))
                .andExpect(content().string(allOf(
                        containsString("#000210"),
                        not(containsString("#000209")),
                        not(containsString("#000220")),
                        containsString("aria-current=\"page\""),
                        containsString("/medico/reportes/citas?desde=2026-11-09&amp;hasta=2026-11-14"
                                + "&amp;estado=ATENDIDA&amp;pagina=3"))));
    }

    @Test
    void reporteCitasAjustaLaPaginaFueraDeRango() throws Exception {
        ReporteCitasDto reporte = citasAtendidas(23);
        when(reporteService.citas(any(), any())).thenReturn(reporte);

        mockMvc.perform(get("/medico/reportes/citas").param("pagina", "99").with(medico()))
                .andExpect(model().attribute("contador", "Mostrando 21–23 de 23 citas"));
        mockMvc.perform(get("/medico/reportes/citas").param("pagina", "abc").with(medico()))
                .andExpect(model().attribute("contador", "Mostrando 1–10 de 23 citas"));
    }

    @Test
    void reporteCitasSinPaginasExtraNoMuestraLaPaginacion() throws Exception {
        ReporteCitasDto reporte = citasDeLaSemana();
        when(reporteService.citas(any(), any())).thenReturn(reporte);

        mockMvc.perform(get("/medico/reportes/citas").with(medico()))
                .andExpect(content().string(not(containsString("reporte-paginacion"))));
    }

    @Test
    void reportePagosPaginaLaTablaPeroElTotalSumaTodoElPeriodo() throws Exception {
        List<PagoValidadoDto> pagos = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            pagos.add(new PagoValidadoDto(LocalDateTime.of(2026, 11, 9, 8, i), 300L + i, "Paciente " + i,
                    TipoDocumentoEnum.DNI, "7000" + (1000 + i), TipoCitaEnum.CONTROL, null, new BigDecimal("80.00")));
        }
        when(reporteService.pagos(any(), any())).thenReturn(new ResumenPagosDto(pagos));

        mockMvc.perform(get("/medico/reportes/pagos").param("desde", "2026-11-09").param("hasta", "2026-11-15")
                        .param("pagina", "2").with(medico()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("rango", "Mostrando 11–12 de 12 pagos"))
                .andExpect(content().string(allOf(
                        containsString("#000310"),
                        not(containsString("#000309")),
                        containsString("Total · 12 pagos"),
                        containsString("S/ 960.00"),
                        containsString("/medico/reportes/pagos?desde=2026-11-09&amp;hasta=2026-11-15&amp;pagina=1"))));
    }

    @Test
    void pacienteNoPuedeAccederALosReportes() throws Exception {
        mockMvc.perform(get("/medico/reportes/citas").with(user("70000001").roles("PACIENTE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/medico/reportes/pagos.xlsx").with(user("70000001").roles("PACIENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioNoAutenticadoEsRedirigidoAlLogin() throws Exception {
        mockMvc.perform(get("/medico/reportes/citas"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    private static RequestPostProcessor medico() {
        return user("10000001").roles("MEDICO");
    }

    private Reporte reporteExportado() {
        ArgumentCaptor<Reporte> reporte = ArgumentCaptor.forClass(Reporte.class);
        verify(exportadorReporte).exportar(reporte.capture());
        return reporte.getValue();
    }

    private static ReporteCitasDto citasDeLaSemana() {
        return new ReporteCitasDto(List.of(
                cita(118L, LUNES, LocalTime.of(9, 30), "Roberto Castro Silva", "70003214", TipoCitaEnum.CONTROL,
                        EstadoCitaEnum.ATENDIDA, EstadoPagoEnum.VALIDADO),
                cita(119L, LUNES, LocalTime.of(11, 0), "Valeria Gómez Morales", "70008765", TipoCitaEnum.NUEVO,
                        EstadoCitaEnum.ATENDIDA, EstadoPagoEnum.VALIDADO),
                cita(129L, LocalDate.of(2026, 11, 11), LocalTime.of(12, 30), "Pedro Huamán Díaz", "70003018",
                        TipoCitaEnum.CONTROL, EstadoCitaEnum.CANCELADA, EstadoPagoEnum.RECHAZADO),
                cita(131L, LocalDate.of(2026, 11, 13), LocalTime.of(12, 0), "María Rojas Vega", "70008193",
                        TipoCitaEnum.CONTROL, EstadoCitaEnum.PENDIENTE_PAGO, null)));
    }

    /** Citas atendidas consecutivas desde la #000200, para probar la paginación. */
    private static ReporteCitasDto citasAtendidas(int cantidad) {
        List<VistaCita> citas = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            citas.add(cita(200L + i, LUNES, LocalTime.of(8, 0).plusMinutes(30L * i), "Paciente " + i, "7000" + (1000 + i),
                    TipoCitaEnum.CONTROL, EstadoCitaEnum.ATENDIDA, EstadoPagoEnum.VALIDADO));
        }
        return new ReporteCitasDto(citas);
    }

    private static ResumenPagosDto pagosDeLaSemana() {
        return new ResumenPagosDto(List.of(
                new PagoValidadoDto(LocalDateTime.of(2026, 11, 9, 8, 40), 118L, "Roberto Castro Silva",
                        TipoDocumentoEnum.DNI, "70003214", TipoCitaEnum.CONTROL, "25790114", new BigDecimal("80.00")),
                new PagoValidadoDto(LocalDateTime.of(2026, 11, 9, 8, 45), 119L, "Valeria Gómez Morales",
                        TipoDocumentoEnum.DNI, "70008765", TipoCitaEnum.NUEVO, null, new BigDecimal("150.00"))));
    }

    private static VistaCita cita(long id, LocalDate fecha, LocalTime inicio, String paciente, String documento,
            TipoCitaEnum tipo, EstadoCitaEnum estado, EstadoPagoEnum estadoPago) {
        VistaCita cita = mock(VistaCita.class);
        when(cita.getIdCita()).thenReturn(id);
        when(cita.getFecha()).thenReturn(fecha);
        when(cita.getHoraInicio()).thenReturn(inicio);
        when(cita.getPaciente()).thenReturn(paciente);
        when(cita.getTipoDocumento()).thenReturn(TipoDocumentoEnum.DNI);
        when(cita.getNumeroDocumento()).thenReturn(documento);
        when(cita.getTipoCita()).thenReturn(tipo);
        when(cita.getEstado()).thenReturn(estado);
        when(cita.getEstadoPago()).thenReturn(estadoPago);
        return cita;
    }
}
