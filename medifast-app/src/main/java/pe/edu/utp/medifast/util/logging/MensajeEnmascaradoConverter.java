package pe.edu.utp.medifast.util.logging;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import pe.edu.utp.medifast.util.Enmascarador;

/**
 * Reemplaza {@code %m}/{@code %msg} en {@code logback-spring.xml}: todo mensaje de log, de la aplicación o de una
 * librería, pasa por {@link Enmascarador#texto(String)} antes de escribirse (RNF07).
 */
public class MensajeEnmascaradoConverter extends MessageConverter {

	@Override
	public String convert(ILoggingEvent evento) {
		return Enmascarador.texto(super.convert(evento));
	}
}
