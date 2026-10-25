package pe.edu.utp.medifast.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Médico del consultorio (tabla {@code medico}). Se carga por script: no hay módulo de administración.
 */
@Entity
@Table(name = "medico")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Medico extends EntidadAuditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_medico")
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_usuario", nullable = false, unique = true, updatable = false)
	private Usuario usuario;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_consultorio", nullable = false)
	private Consultorio consultorio;

	@Column(name = "nombres", nullable = false, length = 100)
	private String nombres;

	@Column(name = "apellidos", nullable = false, length = 100)
	private String apellidos;

	@Column(name = "cmp", nullable = false, unique = true, length = 10)
	private String cmp;

	@Column(name = "especialidad", nullable = false, length = 100)
	private String especialidad;

	@Column(name = "activo", nullable = false)
	@Setter
	private boolean activo = true;

	public Medico(Usuario usuario, Consultorio consultorio, String nombres, String apellidos, String cmp,
			String especialidad) {
		this.usuario = usuario;
		this.consultorio = consultorio;
		this.nombres = nombres;
		this.apellidos = apellidos;
		this.cmp = cmp;
		this.especialidad = especialidad;
	}
}
