package pe.edu.utp.medifast.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;

class ParametrosTest {

	@Test
	void fechaIsoValidaOVacia() {
		assertThat(Parametros.fecha(" 2026-11-09 ")).isEqualTo(LocalDate.of(2026, 11, 9));
		assertThat(Parametros.fecha("09/11/2026")).isNull();
		assertThat(Parametros.fecha("  ")).isNull();
		assertThat(Parametros.fecha(null)).isNull();
	}

	@Test
	void estadoDeLaCitaPorSuNombre() {
		assertThat(Parametros.estadoCita("ATENDIDA")).isEqualTo(EstadoCitaEnum.ATENDIDA);
		assertThat(Parametros.estadoCita("atendida")).isNull();
		assertThat(Parametros.estadoCita(null)).isNull();
	}
}
