package pe.edu.utp.medifast.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.base.Ticker;

class IntentosLoginServiceTest {

	private final AtomicLong tiempoNanos = new AtomicLong();
	private IntentosLoginService service;

	@BeforeEach
	void configurar() {
		service = new IntentosLoginService(new Ticker() {
			@Override
			public long read() {
				return tiempoNanos.get();
			}
		});
	}

	@Test
	void bloqueaAlQuintoIntentoFallido() {
		for (int intento = 0; intento < 4; intento++) {
			service.registrarFallo("70000001");
		}
		assertThat(service.estaBloqueado("70000001")).isFalse();

		service.registrarFallo("70000001");

		assertThat(service.estaBloqueado("70000001")).isTrue();
	}

	@Test
	void desbloqueaAlExpirarLosQuinceMinutos() {
		for (int intento = 0; intento < 5; intento++) {
			service.registrarFallo("70000001");
		}
		tiempoNanos.set(TimeUnit.MINUTES.toNanos(15) + TimeUnit.SECONDS.toNanos(1));

		assertThat(service.estaBloqueado("70000001")).isFalse();
	}

	@Test
	void reiniciarEliminaLosIntentosAcumulados() {
		for (int intento = 0; intento < 5; intento++) {
			service.registrarFallo("70000001");
		}

		service.reiniciar("70000001");

		assertThat(service.estaBloqueado("70000001")).isFalse();
	}
}
