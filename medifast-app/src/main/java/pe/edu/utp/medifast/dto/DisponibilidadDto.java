package pe.edu.utp.medifast.dto;

import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.model.enums.TipoCitaEnum;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Datos de tarifa, ventana de reserva y horarios que puede elegir el paciente.
 */
public record DisponibilidadDto(TipoCitaEnum tipo, int duracionMin, BigDecimal precio, LocalDate fechaMin,
                                LocalDate fechaMax, LocalDate fecha, List<InicioDisponibleDto> inicios,
                                List<BloqueHorarioDto> bloques,
                                Optional<VistaCita> citaActiva) {

  public DisponibilidadDto {
    inicios = List.copyOf(inicios);
    bloques = List.copyOf(bloques);
    citaActiva = citaActiva == null ? Optional.empty() : citaActiva;
  }
}
