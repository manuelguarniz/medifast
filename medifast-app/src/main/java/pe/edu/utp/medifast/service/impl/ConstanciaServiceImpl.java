package pe.edu.utp.medifast.service.impl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.utp.medifast.dao.CitaDao;
import pe.edu.utp.medifast.dao.PagoDao;
import pe.edu.utp.medifast.dto.ConstanciaDto;
import pe.edu.utp.medifast.exception.AccesoDenegadoException;
import pe.edu.utp.medifast.exception.RecursoNoEncontradoException;
import pe.edu.utp.medifast.model.Cita;
import pe.edu.utp.medifast.model.Pago;
import pe.edu.utp.medifast.service.ConstanciaService;

/** Lee las constancias de {@code medifast.uploads-dir}; nunca sirve archivos fuera de esa carpeta (RNF06). */
@Service
public class ConstanciaServiceImpl implements ConstanciaService {

	private final CitaDao citaDao;
	private final PagoDao pagoDao;
	private final Path carpeta;

	public ConstanciaServiceImpl(CitaDao citaDao, PagoDao pagoDao,
			@Value("${medifast.uploads-dir:uploads/comprobantes}") String uploadsDir) {
		this.citaDao = citaDao;
		this.pagoDao = pagoDao;
		this.carpeta = Paths.get(uploadsDir).toAbsolutePath().normalize();
	}

	@Override
	@Transactional(readOnly = true)
	public ConstanciaDto paraPaciente(Long idCita, Long idPaciente) {
		Cita cita = citaDao.findById(idCita)
				.filter(encontrada -> encontrada.getPaciente().getId().equals(idPaciente))
				.orElseThrow(() -> new AccesoDenegadoException("La cita no pertenece al paciente autenticado."));
		return cargar(cita);
	}

	@Override
	@Transactional(readOnly = true)
	public ConstanciaDto paraMedico(Long idCita, Long idMedico) {
		Cita cita = citaDao.findById(idCita)
				.filter(encontrada -> encontrada.getMedico().getId().equals(idMedico))
				.orElseThrow(() -> new AccesoDenegadoException("La cita no pertenece al médico autenticado."));
		return cargar(cita);
	}

	private ConstanciaDto cargar(Cita cita) {
		Pago pago = pagoDao.findByCitaId(cita.getId())
				.orElseThrow(() -> new RecursoNoEncontradoException("La cita no tiene una constancia adjunta."));

		Path archivo = Paths.get(pago.getComprobanteRuta()).toAbsolutePath().normalize();
		if (!archivo.startsWith(carpeta) || !Files.isRegularFile(archivo)) {
			throw new RecursoNoEncontradoException("No se encontró el archivo de la constancia.");
		}
		String nombre = "constancia-%06d.%s".formatted(cita.getId(),
				FilenameUtils.getExtension(archivo.getFileName().toString()));
		return new ConstanciaDto(new FileSystemResource(archivo), pago.getComprobanteTipo(), nombre);
	}
}
