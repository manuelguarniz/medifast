package pe.edu.utp.medifast.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import pe.edu.utp.medifast.dao.CitaDao;
import pe.edu.utp.medifast.dao.HorarioDao;
import pe.edu.utp.medifast.dao.PacienteDao;
import pe.edu.utp.medifast.dao.TarifaDao;
import pe.edu.utp.medifast.dao.VistaCitaDao;
import pe.edu.utp.medifast.exception.FueraDeRangoException;
import pe.edu.utp.medifast.exception.HorarioNoDisponibleException;
import pe.edu.utp.medifast.model.Consultorio;
import pe.edu.utp.medifast.model.Horario;
import pe.edu.utp.medifast.model.Medico;
import pe.edu.utp.medifast.model.Paciente;
import pe.edu.utp.medifast.model.Tarifa;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;
import pe.edu.utp.medifast.model.enums.TipoCitaEnum;
import pe.edu.utp.medifast.service.impl.ReservaServiceImpl;
import pe.edu.utp.medifast.util.Auditoria;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock private CitaDao citaDao;
    @Mock private HorarioDao horarioDao;
    @Mock private TarifaDao tarifaDao;
    @Mock private PacienteDao pacienteDao;
    @Mock private VistaCitaDao vistaCitaDao;
    @Mock private Auditoria auditoria;
    @Mock private Clock clock;

    @InjectMocks
    private ReservaServiceImpl reservaService;

    private final Instant now = Instant.parse("2026-10-26T10:00:00Z");
    private final ZoneId zoneId = ZoneId.of("America/Lima");

    @BeforeEach
    void setUp() {
        lenient().when(clock.getZone()).thenReturn(zoneId);
        lenient().when(clock.instant()).thenReturn(now);
    }

    @Test
    void detectarTipoDevuelveNuevoSinAtenciones() {
        when(citaDao.existsByPacienteIdAndMedicoIdAndEstado(1L, 1L, EstadoCitaEnum.ATENDIDA)).thenReturn(false);
        
        TipoCitaEnum tipo = reservaService.detectarTipo(1L, 1L);
        assertEquals(TipoCitaEnum.NUEVO, tipo);
    }

    @Test
    void detectarTipoDevuelveControlConUnaAtendida() {
        when(citaDao.existsByPacienteIdAndMedicoIdAndEstado(1L, 1L, EstadoCitaEnum.ATENDIDA)).thenReturn(true);
        
        TipoCitaEnum tipo = reservaService.detectarTipo(1L, 1L);
        assertEquals(TipoCitaEnum.CONTROL, tipo);
    }

    @Test
    void disponibilidadFueraDeVentanaLanzaExcepcion() {
        LocalDate hoy = LocalDate.now(clock);
        
        assertThrows(FueraDeRangoException.class, () -> 
            reservaService.disponibilidad(1L, 1L, hoy)
        );
        assertThrows(FueraDeRangoException.class, () -> 
            reservaService.disponibilidad(1L, 1L, hoy.plusDays(31))
        );
    }

    @Test
    void reservarTraduceViolacionDeHorarioActivo() {
        Horario h1 = mock(Horario.class);
        Medico medico = mock(Medico.class);
        
        when(h1.getMedico()).thenReturn(medico);
        when(h1.getFecha()).thenReturn(LocalDate.now(clock).plusDays(1));
        when(h1.isBloqueado()).thenReturn(false);

        when(horarioDao.findById(100L)).thenReturn(Optional.of(h1));
        when(pacienteDao.findById(1L)).thenReturn(Optional.of(mock(Paciente.class)));
        when(citaDao.findFirstByPacienteIdAndEstadoIn(anyLong(), anyList())).thenReturn(Optional.empty());
        when(tarifaDao.findByMedicoIdAndTipoCita(anyLong(), any())).thenReturn(
            Optional.of(new Tarifa(medico, TipoCitaEnum.CONTROL, 30, BigDecimal.valueOf(80)))
        );
        when(horarioDao.bloquesLibres(anyLong(), any())).thenReturn(List.of(h1));

        when(citaDao.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uq_cita_horario_activo"));

        assertThrows(HorarioNoDisponibleException.class, () -> 
            reservaService.reservar(1L, 100L)
        );
    }
}