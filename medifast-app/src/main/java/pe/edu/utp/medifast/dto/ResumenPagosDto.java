package pe.edu.utp.medifast.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import pe.edu.utp.medifast.util.Paginacion;

/**
 * Reporte de pagos verificados (P10, RF09): las filas y sus 3 indicadores. Los indicadores cubren el período completo,
 * aunque {@code pagos} traiga solo una página.
 *
 * @param pagos pagos {@code VALIDADO} de la página, por fecha de validación
 * @param cantidad pagos verificados del período
 * @param total total verificado del período en S/, con 2 decimales
 * @param paginacion página de {@code pagos} dentro del período
 */
public record ResumenPagosDto(List<PagoValidadoDto> pagos, long cantidad, BigDecimal total, Paginacion paginacion) {

	/** Todos los pagos del período en una sola página (exportación); los indicadores salen de esas filas. */
	public ResumenPagosDto(List<PagoValidadoDto> pagos) {
		this(pagos, pagos.size(), pagos.stream().map(PagoValidadoDto::monto).reduce(BigDecimal.ZERO, BigDecimal::add),
				Paginacion.completa(pagos.size()));
	}

	public ResumenPagosDto {
		pagos = List.copyOf(pagos);
		total = (total == null ? BigDecimal.ZERO : total).setScale(2, RoundingMode.HALF_UP);
	}

	/** Total ÷ cantidad, con 2 decimales; S/ 0.00 si no hay pagos. */
	public BigDecimal ticketPromedio() {
		return cantidad == 0
				? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
				: total.divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP);
	}
}
