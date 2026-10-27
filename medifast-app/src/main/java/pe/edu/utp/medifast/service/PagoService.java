package pe.edu.utp.medifast.service;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

import pe.edu.utp.medifast.model.Cita;
import pe.edu.utp.medifast.model.Pago;

public interface PagoService {

	Pago registrarPago(Long citaId, Long idPaciente, String metodoPago, String nroOperacion, MultipartFile comprobante)
			throws IOException;

	Cita obtenerCitaParaPago(Long citaId, Long idPaciente);
}
