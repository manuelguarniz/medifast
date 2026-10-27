package pe.edu.utp.medifast.service.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import pe.edu.utp.medifast.dao.CitaDao;
import pe.edu.utp.medifast.dao.PagoDao;
import pe.edu.utp.medifast.dao.TarifaDao;
import pe.edu.utp.medifast.model.Cita;
import pe.edu.utp.medifast.model.Pago;
import pe.edu.utp.medifast.model.Tarifa;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;
import pe.edu.utp.medifast.service.PagoService;

/** Implementación transaccional del pago simulado; las constancias se guardan en {@code medifast.uploads-dir}. */
@Service
public class PagoServiceImpl implements PagoService {

	private final CitaDao citaDao;
	private final PagoDao pagoDao;
	private final TarifaDao tarifaDao;

	@Value("${medifast.uploads-dir:uploads/comprobantes}")
	private String uploadsDir;

	public PagoServiceImpl(CitaDao citaDao, PagoDao pagoDao, TarifaDao tarifaDao) {
		this.citaDao = citaDao;
		this.pagoDao = pagoDao;
		this.tarifaDao = tarifaDao;
	}

	@Override
	@Transactional(readOnly = true)
	public Cita obtenerCitaParaPago(Long citaId, Long idPaciente) {
		Cita cita = citaDao.findById(citaId)
				.orElseThrow(() -> new EntityNotFoundException("No se encontró la cita."));
		if (!cita.getPaciente().getId().equals(idPaciente)) {
			throw new IllegalArgumentException("La cita no pertenece al paciente autenticado.");
		}
		return cita;
	}

	@Override
	@Transactional
	public Pago registrarPago(Long citaId, Long idPaciente, String metodoPago, String nroOperacion, MultipartFile comprobante)
			throws IOException {
		Cita cita = obtenerCitaParaPago(citaId, idPaciente);
		if (comprobante == null || comprobante.isEmpty()) {
			throw new IllegalArgumentException("Adjunta la constancia del pago.");
		}
		if (cita.getEstado() == EstadoCitaEnum.CANCELADA || cita.getEstado() == EstadoCitaEnum.ATENDIDA) {
			throw new IllegalStateException("La cita no puede pagar en ese estado.");
		}
		if (pagoDao.findByCitaId(citaId).isPresent()) {
			throw new IllegalStateException("La cita ya tiene un pago registrado.");
		}

		String tipoContenido = comprobante.getContentType();
		if (tipoContenido == null || !(tipoContenido.equals("image/jpeg") || tipoContenido.equals("image/png")
				|| tipoContenido.equals("application/pdf"))) {
			throw new IllegalArgumentException("El comprobante debe ser JPG, PNG o PDF.");
		}

		String extension = obtenerExtension(tipoContenido);
		Path destinoDir = Paths.get(uploadsDir).toAbsolutePath().normalize();
		Files.createDirectories(destinoDir);
		String nombreArchivo = UUID.randomUUID() + extension;
		Path destino = destinoDir.resolve(nombreArchivo);
		Files.copy(comprobante.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

		Tarifa tarifa = tarifaDao.findByMedicoIdAndTipoCita(cita.getMedico().getId(), cita.getTipoCita())
				.orElseThrow(() -> new EntityNotFoundException("No existe la tarifa de la cita."));
		BigDecimal monto = tarifa.getPrecio();
		String rutaRelativa = Paths.get(uploadsDir).resolve(nombreArchivo).toString().replace('\\', '/');
		Pago pago = new Pago(cita, monto, nroOperacion, rutaRelativa, tipoContenido, LocalDateTime.now());
		Pago guardado = pagoDao.save(pago);
		cita.setEstado(EstadoCitaEnum.PROGRAMADA);
		citaDao.save(cita);
		return guardado;
	}

	private static String obtenerExtension(String tipoContenido) {
		return switch (tipoContenido) {
			case "image/jpeg" -> ".jpg";
			case "image/png" -> ".png";
			case "application/pdf" -> ".pdf";
			default -> throw new IllegalArgumentException("Tipo de archivo no permitido.");
		};
	}
}
