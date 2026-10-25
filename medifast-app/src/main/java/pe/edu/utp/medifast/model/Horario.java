package pe.edu.utp.medifast.model;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Bloque de atención de un médico en una fecha, generado desde {@link HorarioSemanal} (tabla {@code horario}).
 */
@Entity
@Table(name = "horario", uniqueConstraints = @UniqueConstraint(name = "uq_horario_medico_inicio",
		columnNames = { "id_medico", "fecha", "hora_inicio" }))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Horario extends EntidadAuditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_horario")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_medico", nullable = false, updatable = false)
	private Medico medico;

	@Column(name = "fecha", nullable = false, updatable = false)
	private LocalDate fecha;

	@Column(name = "hora_inicio", nullable = false, updatable = false)
	private LocalTime horaInicio;

	@Column(name = "hora_fin", nullable = false, updatable = false)
	private LocalTime horaFin;

	@Column(name = "bloqueado", nullable = false)
	@Setter
	private boolean bloqueado;

	public Horario(Medico medico, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
		this.medico = medico;
		this.fecha = fecha;
		this.horaInicio = horaInicio;
		this.horaFin = horaFin;
	}
}
