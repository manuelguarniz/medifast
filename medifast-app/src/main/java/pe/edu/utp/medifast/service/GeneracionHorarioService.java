package pe.edu.utp.medifast.service;

import java.time.LocalDate;

/** Genera bloques de atención desde las plantillas semanales activas (RF06). */
public interface GeneracionHorarioService {

	/**
	 * Crea los bloques faltantes del rango de fechas inclusivo.
	 *
	 * @param desde primera fecha a procesar
	 * @param hasta última fecha a procesar
	 * @return cantidad de bloques creados
	 * @throws IllegalArgumentException si el rango está invertido
	 */
	int generar(LocalDate desde, LocalDate hasta);
}
