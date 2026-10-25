package pe.edu.utp.medifast.model;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Parámetros del consultorio: ventana de reserva, tamaño del bloque y tiempo de apartado
 * (tabla {@code consultorio}, RN-04, RN-06).
 */
@Entity
@Table(name = "consultorio")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Consultorio {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_consultorio")
	private Long id;

	@Column(name = "nombre", nullable = false, unique = true, length = 100)
	@Setter
	private String nombre;

	@Column(name = "direccion", length = 200)
	@Setter
	private String direccion;

	@JdbcTypeCode(SqlTypes.TINYINT)
	@Column(name = "dias_anticipacion_min", nullable = false)
	@Setter
	private int diasAnticipacionMin = 1;

	@JdbcTypeCode(SqlTypes.TINYINT)
	@Column(name = "dias_anticipacion_max", nullable = false)
	@Setter
	private int diasAnticipacionMax = 30;

	@JdbcTypeCode(SqlTypes.TINYINT)
	@Column(name = "minutos_bloque", nullable = false)
	@Setter
	private int minutosBloque = 30;

	@JdbcTypeCode(SqlTypes.TINYINT)
	@Column(name = "minutos_apartado", nullable = false)
	@Setter
	private int minutosApartado = 15;

	@Column(name = "activo", nullable = false)
	@Setter
	private boolean activo = true;

	public Consultorio(String nombre, String direccion) {
		this.nombre = nombre;
		this.direccion = direccion;
	}
}
