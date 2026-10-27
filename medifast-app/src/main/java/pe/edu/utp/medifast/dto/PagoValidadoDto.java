package pe.edu.utp.medifast.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import pe.edu.utp.medifast.model.enums.TipoCitaEnum;
import pe.edu.utp.medifast.model.enums.TipoDocumentoEnum;

/**
 * Fila del reporte de pagos (P10, RF09): un pago {@code VALIDADO} con su cita y su paciente.
 *
 * @param validadoEn fecha y hora de la validación; el período del reporte filtra por ella
 * @param idCita cita pagada
 * @param paciente nombres y apellidos del paciente
 * @param tipoDocumento DNI o CE
 * @param numeroDocumento documento completo; la vista y el Excel lo muestran abreviado (RNF07)
 * @param tipoCita NUEVO o CONTROL
 * @param nroOperacion n.º de operación de Yape; opcional (RN-11)
 * @param monto monto guardado al pagar (RN-03)
 */
public record PagoValidadoDto(LocalDateTime validadoEn, Long idCita, String paciente, TipoDocumentoEnum tipoDocumento,
		String numeroDocumento, TipoCitaEnum tipoCita, String nroOperacion, BigDecimal monto) {
}
