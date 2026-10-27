package pe.edu.utp.medifast.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EnmascaradorTest {

	@Test
	void documentoDejaSoloLosUltimosCuatro() {
		assertThat(Enmascarador.documento("45678912")).isEqualTo("****8912");
		assertThat(Enmascarador.documento("001234567AB")).isEqualTo("*******67AB");
	}

	@Test
	void documentoEnPantallaMuestraCuatroPuntosYLosUltimosCuatro() {
		assertThat(Enmascarador.documentoEnPantalla("70009452")).isEqualTo("••••9452");
		assertThat(Enmascarador.documentoEnPantalla("001000026")).isEqualTo("••••0026");
		assertThat(Enmascarador.documentoEnPantalla("123")).isEqualTo("••••");
		assertThat(Enmascarador.documentoEnPantalla(null)).isEqualTo("-");
	}

	@Test
	void valorCortoSeOcultaCompleto() {
		assertThat(Enmascarador.documento("1234")).isEqualTo("****");
	}

	@Test
	void valorVacioSeMuestraComoGuion() {
		assertThat(Enmascarador.documento(null)).isEqualTo("-");
		assertThat(Enmascarador.celular("  ")).isEqualTo("-");
		assertThat(Enmascarador.correo(null)).isEqualTo("-");
	}

	@Test
	void celularDejaSoloLosUltimosCuatro() {
		assertThat(Enmascarador.celular("987654321")).isEqualTo("*****4321");
	}

	@Test
	void correoDejaLaPrimeraLetraYElDominio() {
		assertThat(Enmascarador.correo("juan.perez@gmail.com")).isEqualTo("j***@gmail.com");
		assertThat(Enmascarador.correo("sin-arroba")).isEqualTo("***");
	}

	@Test
	void textoEnmascaraDocumentosCelularesYCorreos() {
		String mensaje = "Login de 45678912, celular 987654321, correo ana.diaz@utp.edu.pe";

		assertThat(Enmascarador.texto(mensaje))
				.isEqualTo("Login de ****8912, celular *****4321, correo a***@utp.edu.pe");
	}

	@Test
	void textoNoTocaIdsFechasNiMontos() {
		String mensaje = "cita=152 pago=87 inicio=2026-10-28T09:00 monto=150.00";

		assertThat(Enmascarador.texto(mensaje)).isEqualTo(mensaje);
	}

	@Test
	void textoYaEnmascaradoNoCambia() {
		String mensaje = "usuario=****8912 correo=j***@gmail.com";

		assertThat(Enmascarador.texto(mensaje)).isEqualTo(mensaje);
	}
}
