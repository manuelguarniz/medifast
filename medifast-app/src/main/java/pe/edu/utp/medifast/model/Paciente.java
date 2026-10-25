package pe.edu.utp.medifast.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos administrativos del paciente (tabla {@code paciente}, RN-10).
 */
@Entity
@Table(name = "paciente")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Paciente {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_paciente")
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_usuario", nullable = false, unique = true, updatable = false)
	private Usuario usuario;

	@Column(name = "nombres", nullable = false, length = 100)
	@Setter
	private String nombres;

	@Column(name = "apellidos", nullable = false, length = 100)
	@Setter
	private String apellidos;

	@Column(name = "fecha_nacimiento", nullable = false)
	@Setter
	private LocalDate fechaNacimiento;

	@Column(name = "celular", nullable = false, length = 9)
	@Setter
	private String celular;

	@Column(name = "correo", nullable = false, unique = true, length = 150)
	@Setter
	private String correo;

	// Valores puestos por MySQL (RNF13)
	@Generated(event = EventType.INSERT)
	@Column(name = "creado_en", insertable = false, updatable = false)
	private LocalDateTime creadoEn;

	@Generated(event = { EventType.INSERT, EventType.UPDATE })
	@Column(name = "actualizado_en", insertable = false, updatable = false)
	private LocalDateTime actualizadoEn;

	public Paciente(Usuario usuario, String nombres, String apellidos, LocalDate fechaNacimiento, String celular,
			String correo) {
		this.usuario = usuario;
		this.nombres = nombres;
		this.apellidos = apellidos;
		this.fechaNacimiento = fechaNacimiento;
		this.celular = celular;
		this.correo = correo;
	}
}
