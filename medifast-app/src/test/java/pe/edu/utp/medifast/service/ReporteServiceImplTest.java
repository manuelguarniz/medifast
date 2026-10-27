package pe.edu.utp.medifast.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.edu.utp.medifast.dao.PagoDao;
import pe.edu.utp.medifast.dao.VistaCitaDao;
import pe.edu.utp.medifast.dto.FiltroReporteDto;
import pe.edu.utp.medifast.dto.PagoValidadoDto;
import pe.edu.utp.medifast.dto.ReporteCitasDto;
import pe.edu.utp.medifast.dto.ResumenPagosDto;
import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;
import pe.edu.utp.medifast.model.enums.TipoCitaEnum;
import pe.edu.utp.medifast.model.enums.TipoDocumentoEnum;
import pe.edu.utp.medifast.service.impl.ReporteServiceImpl;

@ExtendWith(MockitoExtension.class)
class ReporteServiceImplTest {

    @Mock
    private PagoDao pagoDao;

    @Mock
    private VistaCitaDao vistaCitaDao;

    @InjectMocks
    private ReporteServiceImpl reporteService;

    @Test
    void citasCuentaTodosLosEstadosAunqueSeanCero() {
        LocalDate desde = LocalDate.of(2026, 10, 1);
        LocalDate hasta = LocalDate.of(2026, 10, 31);

        VistaCita atendida = cita(EstadoCitaEnum.ATENDIDA);
        VistaCita programada = cita(EstadoCitaEnum.PROGRAMADA);

        when(vistaCitaDao.reporteCitas(1L, desde, hasta, null))
                .thenReturn(List.of(atendida, programada));

        ReporteCitasDto resultado = reporteService.citas(
                1L,
                new FiltroReporteDto(desde, hasta, null));

        assertThat(resultado.filas()).hasSize(2);
        assertThat(resultado.totales()).hasSize(EstadoCitaEnum.values().length);
        assertThat(resultado.total(EstadoCitaEnum.PENDIENTE_PAGO)).isZero();
        assertThat(resultado.total(EstadoCitaEnum.PROGRAMADA)).isEqualTo(1);
        assertThat(resultado.total(EstadoCitaEnum.ATENDIDA)).isEqualTo(1);
        assertThat(resultado.total(EstadoCitaEnum.NO_ASISTIO)).isZero();
        assertThat(resultado.total(EstadoCitaEnum.CANCELADA)).isZero();

        long sumaTotales = resultado.totales().values().stream()
                .mapToLong(Long::longValue)
                .sum();

        assertThat(sumaTotales).isEqualTo(resultado.filas().size());
    }

    @Test
    void citasUsaIdDelMedicoYEstadoDelFiltro() {
        LocalDate desde = LocalDate.of(2026, 10, 1);
        LocalDate hasta = LocalDate.of(2026, 10, 31);

        when(vistaCitaDao.reporteCitas(
                25L,
                desde,
                hasta,
                EstadoCitaEnum.CANCELADA))
                .thenReturn(List.of());

        reporteService.citas(
                25L,
                new FiltroReporteDto(
                        desde,
                        hasta,
                        EstadoCitaEnum.CANCELADA));

        verify(vistaCitaDao).reporteCitas(
                25L,
                desde,
                hasta,
                EstadoCitaEnum.CANCELADA);
    }

    @Test
    void filtroConDesdeMayorQueHastaLanzaExcepcion() {
        assertThatThrownBy(() -> new FiltroReporteDto(
                LocalDate.of(2026, 10, 31),
                LocalDate.of(2026, 10, 1),
                null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La fecha final debe ser igual o posterior a la inicial.");
    }

    @Test
    void filtroSinFechaLanzaExcepcion() {
        assertThatThrownBy(() -> new FiltroReporteDto(null, LocalDate.of(2026, 10, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ingresa la fecha.");
    }

    @Test
    void pagosDevuelveUnaFilaPorPagoYCalculaLosIndicadores() {
        LocalDate desde = LocalDate.of(2026, 11, 9);
        LocalDate hasta = LocalDate.of(2026, 11, 14);
        List<PagoValidadoDto> pagos = List.of(
                pago(118L, "80.00"), pago(119L, "150.00"), pago(121L, "150.00"),
                pago(115L, "150.00"), pago(120L, "80.00"), pago(126L, "80.00"));

        when(pagoDao.pagosValidados(1L, desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay()))
                .thenReturn(pagos);

        ResumenPagosDto resultado = reporteService.pagos(1L, new FiltroReporteDto(desde, hasta));

        assertThat(resultado.pagos()).hasSize(6);
        assertThat(resultado.cantidad()).isEqualTo(6);
        assertThat(resultado.total()).isEqualByComparingTo("690.00");
        assertThat(resultado.ticketPromedio()).isEqualByComparingTo("115.00");
    }

    @Test
    void ticketPromedioRedondeaADosDecimales() {
        ResumenPagosDto resultado = new ResumenPagosDto(List.of(
                pago(1L, "150.00"), pago(2L, "80.00"), pago(3L, "80.00")));

        assertThat(resultado.total()).isEqualByComparingTo("310.00");
        assertThat(resultado.ticketPromedio()).isEqualByComparingTo("103.33");
    }

    @Test
    void sinPagosLosIndicadoresQuedanEnCero() {
        ResumenPagosDto resultado = new ResumenPagosDto(List.of());

        assertThat(resultado.cantidad()).isZero();
        assertThat(resultado.total()).isEqualByComparingTo("0.00");
        assertThat(resultado.ticketPromedio()).isEqualByComparingTo("0.00");
    }

    @Test
    void pagosIncluyeTodoElUltimoDiaDelRango() {
        LocalDate desde = LocalDate.of(2026, 10, 1);
        LocalDate hasta = LocalDate.of(2026, 10, 25);
        LocalDateTime hastaExclusivo = LocalDate.of(2026, 10, 26).atStartOfDay();

        when(pagoDao.pagosValidados(7L, desde.atStartOfDay(), hastaExclusivo))
                .thenReturn(List.of());

        reporteService.pagos(7L, new FiltroReporteDto(desde, hasta));

        verify(pagoDao).pagosValidados(7L, desde.atStartOfDay(), hastaExclusivo);
    }

    private static PagoValidadoDto pago(long idCita, String monto) {
        return new PagoValidadoDto(LocalDateTime.of(2026, 11, 9, 8, 40), idCita, "Roberto Castro Silva",
                TipoDocumentoEnum.DNI, "70003214", TipoCitaEnum.CONTROL, "25790114", new BigDecimal(monto));
    }

    private VistaCita cita(EstadoCitaEnum estado) {
        VistaCita vista = mock(VistaCita.class);
        when(vista.getEstado()).thenReturn(estado);
        return vista;
    }
}
