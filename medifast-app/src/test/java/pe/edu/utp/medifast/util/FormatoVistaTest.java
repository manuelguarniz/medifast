package pe.edu.utp.medifast.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import pe.edu.utp.medifast.model.Medico;
import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.model.enums.TipoCitaEnum;

class FormatoVistaTest {

	@Test
	void inicialesDelNombreYDelApellidoPaterno() {
		assertThat(FormatoVista.iniciales("Andrea Sofía Salazar Pérez")).isEqualTo("AS");
		assertThat(FormatoVista.iniciales("Carlos Mendoza Ríos")).isEqualTo("CM");
		assertThat(FormatoVista.iniciales("Ana Díaz")).isEqualTo("AD");
		assertThat(FormatoVista.iniciales("Ana")).isEqualTo("A");
		assertThat(FormatoVista.iniciales(null)).isEqualTo("?");
	}

	@Test
	void inicialesDeNombresYApellidosSeparados() {
		assertThat(FormatoVista.iniciales("  moisés andrés", "barrantes ríos")).isEqualTo("MB");
	}

	@Test
	void rangoDeHorasRepiteElPeriodoSoloSiCambia() {
		LocalTime diezYMedia = LocalTime.of(10, 30);
		LocalTime onceYMedia = LocalTime.of(11, 30);
		LocalTime doceYMedia = LocalTime.of(12, 30);

		assertThat(FormatoVista.rangoHoras12(diezYMedia, onceYMedia)).isEqualTo("10:30–11:30 a. m.");
		assertThat(FormatoVista.rangoHoras12(onceYMedia, doceYMedia)).isEqualTo("11:30 a. m.–12:30 p. m.");
		assertThat(FormatoVista.rangoHoras12Espaciado(diezYMedia, onceYMedia)).isEqualTo("10:30 – 11:30 a. m.");
		assertThat(FormatoVista.rangoHoras12Espaciado(onceYMedia, doceYMedia))
				.isEqualTo("11:30 a. m. – 12:30 p. m.");
		assertThat(FormatoVista.rangoHoras24(onceYMedia, doceYMedia)).isEqualTo("11:30–12:30");
	}

	@Test
	void montosConSeparadorDeMilesYGuionSiNoHay() {
		assertThat(FormatoVista.soles(new BigDecimal("150"))).isEqualTo("S/ 150.00");
		assertThat(FormatoVista.soles(new BigDecimal("2730.5"))).isEqualTo("S/ 2,730.50");
		assertThat(FormatoVista.solesOGuion(null)).isEqualTo("—");
		assertThat(FormatoVista.solesOGuion(new BigDecimal("80"))).isEqualTo("S/ 80.00");
	}

	@Test
	void cantidadEnSingularOPlural() {
		assertThat(FormatoVista.cantidad(1, "cita", "citas")).isEqualTo("1 cita");
		assertThat(FormatoVista.cantidad(0, "cita", "citas")).isEqualTo("0 citas");
		assertThat(FormatoVista.cantidad(12, "pago", "pagos")).isEqualTo("12 pagos");
	}

	@Test
	void nombreDelMedicoCortoYCompleto() {
		Medico medico = mock(Medico.class);
		when(medico.getNombres()).thenReturn("Moisés Andrés");
		when(medico.getApellidos()).thenReturn("Barrantes Ríos");

		assertThat(FormatoVista.nombreMedico(medico)).isEqualTo("Dr. Moisés Barrantes");
		assertThat(FormatoVista.nombreCompletoMedico(medico)).isEqualTo("Dr. Moisés Andrés Barrantes Ríos");
	}

	@Test
	void fechasCortasSinPuntosYCapitalizadas() {
		LocalDate miercoles = LocalDate.of(2026, 11, 11);
		assertThat(FormatoVista.diaCorto(miercoles)).isEqualTo("Mié 11 nov");
		assertThat(FormatoVista.fechaCorta(miercoles)).isEqualTo("Mié 11 nov 2026");
	}

	@Test
	void tipoYDuracionDeLaCita() {
		VistaCita cita = mock(VistaCita.class);
		when(cita.getHoraInicio()).thenReturn(LocalTime.of(9, 0));
		when(cita.getHoraFin()).thenReturn(LocalTime.of(10, 0));
		when(cita.getTipoCita()).thenReturn(TipoCitaEnum.NUEVO);

		assertThat(FormatoVista.minutos(cita)).isEqualTo(60);
		assertThat(FormatoVista.tipoYDuracion(cita)).isEqualTo("Nuevo · 60 min");
	}
}
