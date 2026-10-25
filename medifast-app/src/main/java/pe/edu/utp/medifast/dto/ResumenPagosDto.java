package pe.edu.utp.medifast.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class ResumenPagosDto {

    private final List<PagoDiaDto> dias;
    private final long cantidadTotal;
    private final BigDecimal montoTotal;
    private final BigDecimal ticketPromedio;

    public ResumenPagosDto(List<PagoDiaDto> dias) {
        this.dias = List.copyOf(dias);
        this.cantidadTotal = dias.stream().mapToLong(PagoDiaDto::getCantidad).sum();
        this.montoTotal = dias.stream()
                .map(PagoDiaDto::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        this.ticketPromedio = cantidadTotal == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : montoTotal.divide(BigDecimal.valueOf(cantidadTotal), 2, RoundingMode.HALF_UP);
    }

    public List<PagoDiaDto> getDias() {
        return dias;
    }

    public long getCantidadTotal() {
        return cantidadTotal;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public BigDecimal getTicketPromedio() {
        return ticketPromedio;
    }
}
