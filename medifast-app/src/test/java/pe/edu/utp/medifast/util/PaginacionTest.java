package pe.edu.utp.medifast.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class PaginacionTest {

	@Test
	void recortaLaPaginaPedida() {
		List<Integer> filas = IntStream.rangeClosed(1, 23).boxed().toList();
		Paginacion paginacion = Paginacion.de("2", filas.size());

		assertThat(paginacion.recortar(filas)).containsExactly(11, 12, 13, 14, 15, 16, 17, 18, 19, 20);
		assertThat(paginacion.paginas()).containsExactly(1, 2, 3);
		assertThat(paginacion.contador("cita", "citas")).isEqualTo("Mostrando 11–20 de 23 citas");
		assertThat(paginacion.tieneAnterior()).isTrue();
		assertThat(paginacion.tieneSiguiente()).isTrue();
	}

	@Test
	void ajustaLaPaginaInvalidaOFueraDeRango() {
		assertThat(Paginacion.de("99", 23).actual()).isEqualTo(3);
		assertThat(Paginacion.de("0", 23).actual()).isEqualTo(1);
		assertThat(Paginacion.de("abc", 23).actual()).isEqualTo(1);
		assertThat(Paginacion.de(null, 23).actual()).isEqualTo(1);
	}

	@Test
	void muestraPuntosSuspensivosEntreLosExtremos() {
		assertThat(Paginacion.de("5", 100).paginas()).containsExactly(1, null, 4, 5, 6, null, 10);
	}

	@Test
	void conUnaSolaPaginaNoHayBarraYElContadorEsElTotal() {
		Paginacion vacia = Paginacion.de(null, 0);
		assertThat(vacia.varias()).isFalse();
		assertThat(vacia.recortar(List.of())).isEmpty();
		assertThat(Paginacion.de(null, 1).contador("cita", "citas")).isEqualTo("1 cita");
		assertThat(Paginacion.de(null, 4).contador("pago", "pagos")).isEqualTo("4 pagos");
	}
}
