package pe.edu.utp.medifast.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityNotFoundException;

import pe.edu.utp.medifast.dao.CitaDao;
import pe.edu.utp.medifast.dao.PagoDao;
import pe.edu.utp.medifast.dao.TarifaDao;
import pe.edu.utp.medifast.dao.VistaCitaDao;
import pe.edu.utp.medifast.dto.PagoPendienteDto;
import pe.edu.utp.medifast.exception.AccesoDenegadoException;
import pe.edu.utp.medifast.exception.AccionNoPermitidaException;
import pe.edu.utp.medifast.exception.PagoInvalidoException;
import pe.edu.utp.medifast.exception.ReservaVencidaException;
import pe.edu.utp.medifast.model.Cita;
import pe.edu.utp.medifast.model.Pago;
import pe.edu.utp.medifast.model.Tarifa;
import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.model.enums.CanceladaPorEnum;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;
import pe.edu.utp.medifast.model.enums.TipoCitaEnum;
import pe.edu.utp.medifast.service.PagoService;
import pe.edu.utp.medifast.util.Auditoria;
import pe.edu.utp.medifast.util.Auditoria.Evento;

/**
 * Implementación transaccional del pago simulado; las constancias se guardan en {@code medifast.uploads-dir} con
 * un nombre UUID y el tipo se decide por los primeros bytes del archivo, no por lo que declara el navegador
 * (RNF06).
 */
@Service
public class PagoServiceImpl implements PagoService {

	static final String CAMPO_COMPROBANTE = "comprobante";
	static final String CAMPO_NRO_OPERACION = "nroOperacion";

	private static final int MAXIMO_NRO_OPERACION = 30;
	private static final byte[] FIRMA_JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };
	private static final byte[] FIRMA_PNG = { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' };
	private static final byte[] FIRMA_PDF = { '%', 'P', 'D', 'F', '-' };

	private final CitaDao citaDao;
	private final PagoDao pagoDao;
	private final TarifaDao tarifaDao;
	private final VistaCitaDao vistaCitaDao;
	private final Auditoria auditoria;
	private final Clock clock;
	private final String uploadsDir;

	public PagoServiceImpl(CitaDao citaDao, PagoDao pagoDao, TarifaDao tarifaDao, VistaCitaDao vistaCitaDao,
			Auditoria auditoria, Clock clock, @Value("${medifast.uploads-dir:uploads/comprobantes}") String uploadsDir) {
		this.citaDao = citaDao;
		this.pagoDao = pagoDao;
		this.tarifaDao = tarifaDao;
		this.vistaCitaDao = vistaCitaDao;
		this.auditoria = auditoria;
		this.clock = clock;
		this.uploadsDir = uploadsDir;
	}

	@Override
	@Transactional(readOnly = true)
	public PagoPendienteDto paraPagar(Long citaId, Long idPaciente) {
		VistaCita cita = vistaCitaDao.findById(citaId)
				.filter(encontrada -> encontrada.getIdPaciente().equals(idPaciente))
				.orElseThrow(() -> new AccesoDenegadoException("La cita no pertenece al paciente autenticado."));
		long segundos = cita.getEstado() == EstadoCitaEnum.PENDIENTE_PAGO && cita.getExpiraEn() != null
				? Math.max(0, Duration.between(LocalDateTime.now(clock), cita.getExpiraEn()).toSeconds())
				: 0;
		return new PagoPendienteDto(cita, tarifa(cita.getIdMedico(), cita.getTipoCita()).getPrecio(), segundos);
	}

	@Override
	@Transactional
	public Pago registrarPago(Long citaId, Long idPaciente, String nroOperacion, MultipartFile comprobante)
			throws IOException {
		Cita cita = citaDao.findParaPagarById(citaId)
				.filter(encontrada -> encontrada.getPaciente().getId().equals(idPaciente))
				.orElseThrow(() -> new AccesoDenegadoException("La cita no pertenece al paciente autenticado."));
		LocalDateTime ahora = LocalDateTime.now(clock);
		verificarPendiente(cita, ahora);

		String operacion = StringUtils.trimToNull(nroOperacion);
		Map<String, String> errores = new LinkedHashMap<>();
		String tipo = validarComprobante(comprobante, errores);
		validarNroOperacion(operacion, errores);
		if (!errores.isEmpty()) {
			throw new PagoInvalidoException(errores);
		}
		if (pagoDao.findByCitaId(citaId).isPresent()) {
			throw new AccionNoPermitidaException("La cita ya tiene un pago registrado.");
		}

		Path ruta = guardar(comprobante, tipo);
		Pago pago = pagoDao.save(new Pago(cita, tarifa(cita.getMedico().getId(), cita.getTipoCita()).getPrecio(),
				operacion, ruta.toString().replace('\\', '/'), tipo, ahora));
		cita.setEstado(EstadoCitaEnum.PROGRAMADA);
		auditoria.registrar(Evento.CONSTANCIA, "id_cita=" + citaId);
		return pago;
	}

	/** RF03 y RN-04: solo se paga una reserva {@code PENDIENTE_PAGO} cuyo plazo no venció. */
	private static void verificarPendiente(Cita cita, LocalDateTime ahora) {
		boolean canceladaPorSistema = cita.getEstado() == EstadoCitaEnum.CANCELADA
				&& cita.getCanceladaPor() == CanceladaPorEnum.SISTEMA;
		boolean pendienteVencida = cita.getEstado() == EstadoCitaEnum.PENDIENTE_PAGO
				&& (cita.getExpiraEn() == null || !cita.getExpiraEn().isAfter(ahora));
		if (canceladaPorSistema || pendienteVencida) {
			throw new ReservaVencidaException();
		}
		if (cita.getEstado() != EstadoCitaEnum.PENDIENTE_PAGO) {
			throw new AccionNoPermitidaException("Esta cita ya no espera un pago.");
		}
	}

	/** RN-11 y RNF06: obligatoria, hasta 5 MB y JPG, PNG o PDF según su contenido real. */
	private static String validarComprobante(MultipartFile comprobante, Map<String, String> errores)
			throws IOException {
		if (comprobante == null || comprobante.isEmpty()) {
			errores.put(CAMPO_COMPROBANTE, "Adjunta la constancia de pago.");
			return null;
		}
		if (comprobante.getSize() > TAMANO_MAXIMO) {
			errores.put(CAMPO_COMPROBANTE, "El archivo supera los 5 MB.");
			return null;
		}
		String tipo = tipoReal(comprobante);
		if (tipo == null) {
			errores.put(CAMPO_COMPROBANTE, "Formato no permitido. Usa JPG, PNG o PDF.");
		}
		return tipo;
	}

	/** RF03: opcional; si se ingresa, solo dígitos y hasta 30 (columna {@code pago.nro_operacion}). */
	private static void validarNroOperacion(String operacion, Map<String, String> errores) {
		if (operacion == null) {
			return;
		}
		if (!StringUtils.isNumeric(operacion)) {
			errores.put(CAMPO_NRO_OPERACION, "Ingresa solo números.");
		} else if (operacion.length() > MAXIMO_NRO_OPERACION) {
			errores.put(CAMPO_NRO_OPERACION, "El n.º de operación no puede superar los 30 dígitos.");
		}
	}

	private static String tipoReal(MultipartFile comprobante) throws IOException {
		byte[] cabecera;
		try (InputStream entrada = comprobante.getInputStream()) {
			cabecera = entrada.readNBytes(FIRMA_PNG.length);
		}
		if (empiezaCon(cabecera, FIRMA_JPEG)) {
			return "image/jpeg";
		}
		if (empiezaCon(cabecera, FIRMA_PNG)) {
			return "image/png";
		}
		return empiezaCon(cabecera, FIRMA_PDF) ? "application/pdf" : null;
	}

	private static boolean empiezaCon(byte[] datos, byte[] firma) {
		return datos.length >= firma.length && Arrays.equals(datos, 0, firma.length, firma, 0, firma.length);
	}

	/** Guarda el archivo con nombre UUID; si la transacción se revierte, lo borra para no dejar huérfanos. */
	private Path guardar(MultipartFile comprobante, String tipo) throws IOException {
		Path carpeta = Paths.get(uploadsDir);
		Files.createDirectories(carpeta);
		Path ruta = carpeta.resolve(UUID.randomUUID() + extension(tipo));
		try (InputStream entrada = comprobante.getInputStream()) {
			FileUtils.copyInputStreamToFile(entrada, ruta.toFile());
		}
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCompletion(int estado) {
					if (estado == STATUS_ROLLED_BACK) {
						FileUtils.deleteQuietly(ruta.toFile());
					}
				}
			});
		}
		return ruta;
	}

	private static String extension(String tipo) {
		return switch (tipo) {
			case "image/jpeg" -> ".jpg";
			case "image/png" -> ".png";
			default -> ".pdf";
		};
	}

	private Tarifa tarifa(Long idMedico, TipoCitaEnum tipo) {
		return tarifaDao.findByMedicoIdAndTipoCita(idMedico, tipo)
				.orElseThrow(() -> new EntityNotFoundException("No existe la tarifa de la cita."));
	}
}
