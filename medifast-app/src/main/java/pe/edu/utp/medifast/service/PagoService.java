package pe.edu.utp.medifast.service;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

import pe.edu.utp.medifast.dto.PagoPendienteDto;
import pe.edu.utp.medifast.model.Pago;

/** Reglas de negocio del pago simulado con Yape y de la constancia adjunta (RF03, RN-04, RN-11, RNF06). */
public interface PagoService {

	/** Tamaño máximo de la constancia (RN-11). */
	long TAMANO_MAXIMO = 5L * 1024 * 1024;

	/**
	 * Cita que el paciente va a pagar en P3, con la tarifa y el tiempo que le queda.
	 *
	 * @param citaId cita solicitada
	 * @param idPaciente paciente autenticado
	 * @return cita, monto y segundos restantes
	 * @throws pe.edu.utp.medifast.exception.AccesoDenegadoException si la cita no existe o es de otro paciente
	 */
	PagoPendienteDto paraPagar(Long citaId, Long idPaciente);

	/**
	 * Registra el pago simulado: valida la constancia por su contenido real (JPG, PNG o PDF de hasta 5 MB), la
	 * guarda con un nombre UUID fuera de {@code static/}, crea el pago {@code POR_VALIDAR} con el monto de la tarifa
	 * vigente y deja la cita {@code PROGRAMADA}, todo en una transacción que bloquea la cita para que el evento de
	 * RF16 no la cancele a la vez.
	 *
	 * @param citaId cita que se paga
	 * @param idPaciente paciente autenticado; debe ser el dueño de la cita
	 * @param nroOperacion n.º de operación de Yape; opcional, solo dígitos
	 * @param comprobante constancia adjunta
	 * @return pago creado en estado {@code POR_VALIDAR}
	 * @throws pe.edu.utp.medifast.exception.AccesoDenegadoException si la cita no existe o es de otro paciente
	 * @throws pe.edu.utp.medifast.exception.ReservaVencidaException si pasaron los 15 minutos (RN-04)
	 * @throws pe.edu.utp.medifast.exception.AccionNoPermitidaException si la cita ya no espera el pago
	 * @throws pe.edu.utp.medifast.exception.PagoInvalidoException si la constancia o el n.º de operación no son
	 *         válidos
	 * @throws IOException si no se puede guardar el archivo
	 */
	Pago registrarPago(Long citaId, Long idPaciente, String nroOperacion, MultipartFile comprobante)
			throws IOException;
}
