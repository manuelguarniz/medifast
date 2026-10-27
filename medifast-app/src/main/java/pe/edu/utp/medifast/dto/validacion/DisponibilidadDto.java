package pe.edu.utp.medifast.dto.validacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.model.enums.TipoCitaEnum;

public record DisponibilidadDto(
    TipoCitaEnum tipo,
    int duracionMin,
    BigDecimal precio,
    LocalDate fechaMin,
    LocalDate fechaMax,
    LocalDate fecha,
    List<InicioDisponibleDto> inicios,
    Optional<VistaCita> citaActiva
) {}