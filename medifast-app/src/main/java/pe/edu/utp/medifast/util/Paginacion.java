package pe.edu.utp.medifast.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Página de un listado (reportes P9 y P10, agenda P7 e historial P5). {@code inicio} y {@code fin} delimitan las
 * filas (fin exclusivo); {@code paginas} trae los números a mostrar, con {@code null} donde va "…" (siempre la
 * primera, la última y las vecinas de la actual). Los listados que pueden crecer sin límite (P5, P9, P10) leen de la
 * BD solo la página con {@link #pageable()}; la agenda (P7), acotada a un día o una semana, la recorta en memoria con
 * {@link #recortar(List)}.
 *
 * @param actual página mostrada, desde 1
 * @param totalPaginas al menos 1, aunque no haya filas
 * @param totalFilas filas de todo el listado
 * @param inicio índice de la primera fila de la página
 * @param fin índice siguiente a la última fila de la página
 * @param paginas números de la barra de paginación
 * @param porPagina filas por página
 */
public record Paginacion(int actual, int totalPaginas, int totalFilas, int inicio, int fin, List<Integer> paginas,
		int porPagina) {

	/** Filas por página en todos los listados paginados. */
	public static final int POR_PAGINA = 10;

	/** Una página inválida o fuera de rango se ajusta a la más cercana. */
	public static Paginacion de(String pagina, int totalFilas) {
		return de(pagina, totalFilas, POR_PAGINA);
	}

	static Paginacion de(String pagina, int totalFilas, int porPagina) {
		int totalPaginas = Math.max(1, (totalFilas + porPagina - 1) / porPagina);
		int actual = Math.clamp(leer(pagina), 1, totalPaginas);
		int inicio = (actual - 1) * porPagina;
		List<Integer> paginas = new ArrayList<>();
		for (int n = 1; n <= totalPaginas; n++) {
			if (n == 1 || n == totalPaginas || Math.abs(n - actual) <= 1) {
				paginas.add(n);
			} else if (paginas.get(paginas.size() - 1) != null) {
				paginas.add(null);
			}
		}
		return new Paginacion(actual, totalPaginas, totalFilas, inicio, Math.min(inicio + porPagina, totalFilas),
				Collections.unmodifiableList(paginas), porPagina);
	}

	/** El listado completo en una sola página (exportación a Excel). */
	public static Paginacion completa(int totalFilas) {
		return de(null, totalFilas, Math.max(1, totalFilas));
	}

	private static int leer(String pagina) {
		try {
			return Integer.parseInt(StringUtils.strip(pagina));
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	/** La página actual como {@code LIMIT}/{@code OFFSET} para una consulta de Spring Data. */
	public Pageable pageable() {
		return PageRequest.of(actual - 1, porPagina);
	}

	/** Filas de la página actual; {@code filas} debe ser el listado completo. */
	public <T> List<T> recortar(List<T> filas) {
		return filas.subList(inicio, fin);
	}

	/** "4 citas" con una sola página; "Mostrando 11–20 de 23 citas" con varias. */
	public String contador(String singular, String plural) {
		String total = totalFilas == 1 ? "1 " + singular : totalFilas + " " + plural;
		return varias() ? "Mostrando " + (inicio + 1) + "–" + fin + " de " + total : total;
	}

	public boolean varias() {
		return totalPaginas > 1;
	}

	public boolean tieneAnterior() {
		return actual > 1;
	}

	public boolean tieneSiguiente() {
		return actual < totalPaginas;
	}
}
