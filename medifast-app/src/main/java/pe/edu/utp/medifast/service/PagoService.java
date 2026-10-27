package pe.edu.utp.medifast.service;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

import pe.edu.utp.medifast.model.Cita;
import pe.edu.utp.medifast.model.Pago;

/** Reglas de negocio del pago simulado con Yape y de la constancia adjunta (RF03, RN-11). */
public interface PagoService {

	/**
	 * Registra el pago simulado de una cita: guarda la constancia con un nombre UUID fuera de {@code static/},
	 * crea el pago {@code POR_VALIDAR} con el monto de la tarifa vigente y deja la cita {@code PROGRAMADA}.
	 *
	 * @param citaId cita que se paga
	 * @param idPaciente paciente autenticado; debe ser el dueño de la cita
	 * @param metodoPago método enviado por el formulario; el pago se guarda siempre como {@code YAPE_SIMULADO}
	 * @param nroOperacion n.º de operación de Yape; es opcional
	 * @param comprobante constancia JPG, PNG o PDF
	 * @return pago creado en estado {@code POR_VALIDAR}
	 * @throws IllegalArgumentException si la cita no es del paciente, falta la constancia o su tipo no está permitido
	 * @throws IllegalStateException si la cita está cancelada o atendida, o ya tiene un pago
	 * @throws jakarta.persistence.EntityNotFoundException si no existen la cita o su tarifa
	 * @throws IOException si no se puede guardar el archivo
	 */
	Pago registrarPago(Long citaId, Long idPaciente, String metodoPago, String nroOperacion, MultipartFile comprobante)
			throws IOException;

	/**
	 * Obtiene la cita que el paciente va a pagar, o cuya confirmación consulta, verificando que le pertenezca.
	 *
	 * @param citaId cita solicitada
	 * @param idPaciente paciente autenticado
	 * @return cita del paciente
	 * @throws jakarta.persistence.EntityNotFoundException si la cita no existe
	 * @throws IllegalArgumentException si la cita pertenece a otro paciente
	 */
	Cita obtenerCitaParaPago(Long citaId, Long idPaciente);
}
