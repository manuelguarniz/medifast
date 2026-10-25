package pe.edu.utp.medifast.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class PagoDiaDto {
    private final LocalDate fecha;
    private final long cantidad;
    private final BigDecimal total;

    public PagoDiaDto(LocalDate fecha, long cantidad, BigDecimal total) {
        this.fecha = fecha;
        this.cantidad = cantidad;
        this.total = total;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public long getCantidad() {
        return cantidad;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getFechaFormateada() {
        return fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}
