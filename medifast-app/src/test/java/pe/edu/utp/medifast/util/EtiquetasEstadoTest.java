package pe.edu.utp.medifast.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import org.junit.jupiter.api.Test;

import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;
import pe.edu.utp.medifast.model.enums.EstadoPagoEnum;

class EtiquetasEstadoTest {

	@Test
	void opcionesDelFiltroEnElOrdenDelEnum() {
		assertThat(EtiquetasEstado.OPCIONES_CITA).containsExactly(
				entry("PENDIENTE_PAGO", "Pago pendiente"),
				entry("PROGRAMADA", "Confirmada"),
				entry("ATENDIDA", "Atendida"),
				entry("NO_ASISTIO", "No asistió"),
				entry("CANCELADA", "Cancelada"));
	}

	@Test
	void citaVencidaTieneSuPropiaEtiquetaYColor() {
		assertThat(EtiquetasEstado.cita(EstadoCitaEnum.PENDIENTE_PAGO, true)).isEqualTo("Vencida");
		assertThat(EtiquetasEstado.claseCita(EstadoCitaEnum.PENDIENTE_PAGO, true)).isEqualTo("vencida");
		assertThat(EtiquetasEstado.cita(EstadoCitaEnum.PENDIENTE_PAGO, false)).isEqualTo("Pago pendiente");
		assertThat(EtiquetasEstado.claseCita(EstadoCitaEnum.PROGRAMADA, false)).isEqualTo("confirmada");
	}

	@Test
	void pagoSinRegistroEsGuionONulo() {
		assertThat(EtiquetasEstado.pago(null)).isEqualTo("—");
		assertThat(EtiquetasEstado.pagoSiExiste(null)).isNull();
		assertThat(EtiquetasEstado.pagoSiExiste(EstadoPagoEnum.VALIDADO)).isEqualTo("Verificado");
	}
}
