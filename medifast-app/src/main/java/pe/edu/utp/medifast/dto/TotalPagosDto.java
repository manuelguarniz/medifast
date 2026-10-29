package pe.edu.utp.medifast.dto;

import java.math.BigDecimal;

/**
 * Cantidad y suma de los pagos validados de un período (indicadores de P10).
 *
 * @param cantidad pagos validados
 * @param total suma de sus montos; {@code null} si no hay pagos
 */
public record TotalPagosDto(long cantidad, BigDecimal total) {
}
