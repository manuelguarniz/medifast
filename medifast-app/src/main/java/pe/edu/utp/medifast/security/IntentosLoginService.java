package pe.edu.utp.medifast.security;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Lleva intentos fallidos por documento en memoria; el contador expira tras quince minutos sin actividad.
 */
@Service
public class IntentosLoginService {

	private static final int MAX_INTENTOS = 5;
	private final Cache<String, Integer> intentos;

	public IntentosLoginService() {
		this(Ticker.systemTicker());
	}

	IntentosLoginService(Ticker ticker) {
		intentos = CacheBuilder.newBuilder()
				.expireAfterWrite(15, TimeUnit.MINUTES)
				.ticker(ticker)
				.<String, Integer>build();
	}

	/** Registra un fallo de autenticación para el documento indicado. */
	public void registrarFallo(String numeroDocumento) {
		intentos.asMap().compute(numeroDocumento, (documento, contador) -> contador == null ? 1 : contador + 1);
	}

	/** Indica si el documento alcanzó el límite de intentos fallidos aún vigentes. */
	public boolean estaBloqueado(String numeroDocumento) {
		Integer contador = intentos.getIfPresent(numeroDocumento);
		return contador != null && contador >= MAX_INTENTOS;
	}

	/** Elimina el contador de fallos tras una autenticación correcta. */
	public void reiniciar(String numeroDocumento) {
		intentos.invalidate(numeroDocumento);
	}
}
