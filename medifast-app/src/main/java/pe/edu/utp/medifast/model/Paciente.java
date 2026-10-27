package pe.edu.utp.medifast.model;

import java.time.LocalDate;

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

import pe.edu.utp.medifast.dto.PerfilDto;

/**
 * Datos administrativos del paciente (tabla {@code paciente}, RN-10).
 */
@Entity
@Table(name = "paciente")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Paciente extends EntidadAuditable {

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

	public Paciente(Usuario usuario, String nombres, String apellidos, LocalDate fechaNacimiento, String celular,
			String correo) {
		this.usuario = usuario;
		this.nombres = nombres;
		this.apellidos = apellidos;
		this.fechaNacimiento = fechaNacimiento;
		this.celular = celular;
		this.correo = correo;
	}

	/** Reemplaza los datos personales editables en "Mi perfil" (RF11); el usuario y su documento no cambian. */
	public void actualizarDatos(PerfilDto datos) {
		this.nombres = datos.nombres();
		this.apellidos = datos.apellidos();
		this.fechaNacimiento = datos.fechaNacimiento();
		this.celular = datos.celular();
		this.correo = datos.correo();
	}
}
