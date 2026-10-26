package pe.edu.utp.medifast.service;

import java.time.LocalDate;

/**
 * Genera bloques diarios de atención a partir de las plantillas semanales (RF06, RN-08).
 */
public interface GeneracionHorarioService {

	/**
	 * Crea los bloques faltantes para los médicos activos y fechas del rango, ambos inclusivos.
	 * El tamaño de cada bloque se obtiene del consultorio del médico y los bloques existentes
	 * se conservan sin duplicarlos.
	 *
	 * @param desde primera fecha a procesar, inclusive
	 * @param hasta última fecha a procesar, inclusive
	 * @return cantidad de bloques creados
	 * @throws IllegalArgumentException si {@code desde} es posterior a {@code hasta}
	 * @throws NullPointerException si alguna fecha es nula
	 */
	int generar(LocalDate desde, LocalDate hasta);
}
