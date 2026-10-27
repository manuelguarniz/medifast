package pe.edu.utp.medifast.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Reporte de pagos verificados (P10, RF09): las filas y sus 3 indicadores, calculados sobre esas mismas filas.
 *
 * @param pagos pagos {@code VALIDADO} del período, por fecha de validación
 */
public record ResumenPagosDto(List<PagoValidadoDto> pagos) {

	public ResumenPagosDto {
		pagos = List.copyOf(pagos);
	}

	/** Cantidad de pagos verificados. */
	public long cantidad() {
		return pagos.size();
	}

	/** Total verificado en S/, con 2 decimales. */
	public BigDecimal total() {
		return pagos.stream()
				.map(PagoValidadoDto::monto)
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.setScale(2, RoundingMode.HALF_UP);
	}

	/** Total ÷ cantidad, con 2 decimales; S/ 0.00 si no hay pagos. */
	public BigDecimal ticketPromedio() {
		return pagos.isEmpty()
				? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
				: total().divide(BigDecimal.valueOf(pagos.size()), 2, RoundingMode.HALF_UP);
	}
}
