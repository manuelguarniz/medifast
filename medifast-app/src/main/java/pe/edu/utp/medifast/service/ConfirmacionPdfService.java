package pe.edu.utp.medifast.service;

/** Confirmación de la cita en PDF (RF15, SD-08), generada con OpenPDF. */
public interface ConfirmacionPdfService {

	/**
	 * Genera la confirmación con n.º de cita, médico, fecha, hora, tipo, monto y estado del pago.
	 *
	 * @param idCita cita solicitada
	 * @param idPaciente paciente autenticado; debe ser el dueño
	 * @return bytes del PDF
	 * @throws pe.edu.utp.medifast.exception.AccesoDenegadoException si la cita no es del paciente o no está
	 *         {@code PROGRAMADA} ni {@code ATENDIDA}
	 */
	byte[] generar(Long idCita, Long idPaciente);
}
