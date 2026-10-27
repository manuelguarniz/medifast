package pe.edu.utp.medifast.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Registra los eventos de auditoría en {@code logs/auditoria.log} (RNF13). Cada línea lleva el evento, el usuario
 * enmascarado, la IP de origen y un detalle con IDs internos: nunca contraseñas, documentos completos ni nombres
 * (RNF07).
 */
@Component
public class Auditoria {

	private static final Logger LOG = LoggerFactory.getLogger("AUDITORIA");
	private static final int ANCHO_EVENTO = 15;
	private static final String SIN_DATO = "-";

	public enum Evento {
		LOGIN_OK, LOGIN_FALLIDO, LOGOUT, REGISTRO, RESERVA, PAGO_VALIDADO, PAGO_RECHAZADO, CANCELACION, PERFIL, CONTRASENA
	}

	/** Registra un evento del usuario autenticado en la petición actual. */
	public void registrar(Evento evento, String detalle) {
		Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
		registrar(evento, autenticacion != null ? autenticacion.getName() : null, detalle);
	}

	/** Registra un evento de un usuario dado por su n.º de documento (p. ej. durante el login). */
	public void registrar(Evento evento, String numeroDocumento, String detalle) {
		String linea = String.join(" ", StringUtils.rightPad(evento.name(), ANCHO_EVENTO),
				"usuario=" + Enmascarador.documento(numeroDocumento), "ip=" + ipActual(),
				StringUtils.defaultString(detalle));
		LOG.info(linea.strip());
	}

	private static String ipActual() {
		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
			return atributos.getRequest().getRemoteAddr();
		}
		return SIN_DATO;
	}
}
