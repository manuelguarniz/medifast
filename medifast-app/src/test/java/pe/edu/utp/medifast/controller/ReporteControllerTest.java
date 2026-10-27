package pe.edu.utp.medifast.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pe.edu.utp.medifast.config.SecurityConfig;
import pe.edu.utp.medifast.dto.FiltroReporteDto;
import pe.edu.utp.medifast.dto.ReporteCitasDto;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;
import pe.edu.utp.medifast.security.RedireccionPorRol;
import pe.edu.utp.medifast.service.MedicoService;
import pe.edu.utp.medifast.service.ReporteService;
import pe.edu.utp.medifast.util.ExportadorReporte;

@WebMvcTest(ReporteController.class)
@Import({ SecurityConfig.class, RedireccionPorRol.class })
class ReporteControllerTest {

    private static final String XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReporteService reporteService;

    @MockitoBean
    private MedicoService medicoService;

    @MockitoBean
    private ExportadorReporte exportadorReporte;

    @Test
    @WithMockUser(username = "10000001", roles = "MEDICO")
    void reporteCitasDevuelveVistaYModelo() throws Exception {
        LocalDate desde = LocalDate.of(2026, 10, 1);
        LocalDate hasta = LocalDate.of(2026, 10, 31);
        ReporteCitasDto reporte = new ReporteCitasDto(List.of());

        when(medicoService.obtenerIdPorDocumento("10000001"))
                .thenReturn(1L);
        when(reporteService.citas(
                1L,
                new FiltroReporteDto(desde, hasta, EstadoCitaEnum.ATENDIDA)))
                .thenReturn(reporte);

        mockMvc.perform(get("/medico/reportes/citas")
                        .param("desde", "2026-10-01")
                        .param("hasta", "2026-10-31")
                        .param("estado", "ATENDIDA"))
                .andExpect(status().isOk())
                .andExpect(view().name("medico/reporte-citas"))
                .andExpect(model().attribute("reporte", reporte))
                .andExpect(model().attribute("desde", desde))
                .andExpect(model().attribute("hasta", hasta))
                .andExpect(model().attribute("estado", EstadoCitaEnum.ATENDIDA))
                .andExpect(model().attribute("pendientesPago", 0L))
                .andExpect(model().attribute("programadas", 0L))
                .andExpect(model().attribute("atendidas", 0L))
                .andExpect(model().attribute("noAsistieron", 0L))
                .andExpect(model().attribute("canceladas", 0L));
    }

    @Test
    @WithMockUser(username = "10000001", roles = "MEDICO")
    void reporteCitasSinFechasUsaSemanaActual() throws Exception {
        LocalDate hoy = LocalDate.now();
        LocalDate inicioSemana = hoy.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate finSemana = inicioSemana.plusDays(6);
        ReporteCitasDto reporte = new ReporteCitasDto(List.of());

        when(medicoService.obtenerIdPorDocumento("10000001"))
                .thenReturn(1L);
        when(reporteService.citas(
                1L,
                new FiltroReporteDto(inicioSemana, finSemana, null)))
                .thenReturn(reporte);

        mockMvc.perform(get("/medico/reportes/citas"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("desde", inicioSemana))
                .andExpect(model().attribute("hasta", finSemana));
    }

    @Test
    @WithMockUser(username = "10000001", roles = "MEDICO")
    void exportarCitasXlsxDevuelve200YContentTypeExcel() throws Exception {
        LocalDate desde = LocalDate.of(2026, 10, 1);
        LocalDate hasta = LocalDate.of(2026, 10, 31);
        ReporteCitasDto reporte = new ReporteCitasDto(List.of());
        byte[] archivo = new byte[]{1, 2, 3};

        when(medicoService.obtenerIdPorDocumento("10000001"))
                .thenReturn(1L);
        when(reporteService.citas(
                1L,
                new FiltroReporteDto(desde, hasta, null)))
                .thenReturn(reporte);
        when(exportadorReporte.exportar(any()))
                .thenReturn(archivo);

        mockMvc.perform(get("/medico/reportes/citas.xlsx")
                        .param("desde", "2026-10-01")
                        .param("hasta", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(XLSX))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"reporte-citas-2026-10-01-2026-10-31.xlsx\""))
                .andExpect(content().bytes(archivo));
    }


    @Test
    @WithMockUser(username = "10000001", roles = "MEDICO")
    void rangoInvalidoMuestraMensajeEnLaVista() throws Exception {
        mockMvc.perform(get("/medico/reportes/citas")
                        .param("desde", "2026-10-31")
                        .param("hasta", "2026-10-01"))
                .andExpect(status().isOk())
                .andExpect(view().name("medico/reporte-citas"))
                .andExpect(model().attribute(
                        "error",
                        "La fecha desde no puede ser posterior a la fecha hasta"));
    }

    @Test
    @WithMockUser(username = "70000001", roles = "PACIENTE")
    void pacienteNoPuedeAccederAlReporteDeCitas() throws Exception {
        mockMvc.perform(get("/medico/reportes/citas"))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioNoAutenticadoEsRedirigidoAlLogin() throws Exception {
        mockMvc.perform(get("/medico/reportes/citas"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}
