package pe.edu.utp.medifast.controller;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import pe.edu.utp.medifast.dto.ConstanciaDto;

/** Respuestas HTTP con archivos: constancias que se abren en el navegador y documentos que se descargan. */
final class Respuestas {

	static final MediaType XLSX = MediaType.parseMediaType(
			"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

	private Respuestas() {
	}

	/** RF12: la constancia se abre en una pestaña nueva con su tipo real y su nombre. */
	static ResponseEntity<Resource> constancia(ConstanciaDto constancia) {
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(constancia.tipo()))
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.inline().filename(constancia.nombre()).build().toString())
				.body(constancia.archivo());
	}

	/** Descarga el archivo con el nombre indicado (PDF de la confirmación, reportes .xlsx). */
	static ResponseEntity<byte[]> descarga(byte[] archivo, MediaType tipo, String nombreArchivo) {
		return ResponseEntity.ok()
				.contentType(tipo)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(nombreArchivo).build().toString())
				.body(archivo);
	}
}
