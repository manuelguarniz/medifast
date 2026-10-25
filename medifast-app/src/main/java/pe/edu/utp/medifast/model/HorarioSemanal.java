package pe.edu.utp.medifast.model;

import java.time.DayOfWeek;
import java.time.LocalTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import pe.edu.utp.medifast.util.converter.DiaSemanaConverter;

/**
 * Plantilla de atención semanal del médico; de ella se generan los bloques de {@link Horario}
 * (tabla {@code horario_semanal}, RN-08).
 */
@Entity
@Table(name = "horario_semanal")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HorarioSemanal {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_horario_semanal")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_medico", nullable = false, updatable = false)
	private Medico medico;

	@Convert(converter = DiaSemanaConverter.class)
	@JdbcTypeCode(SqlTypes.TINYINT)
	@Column(name = "dia_semana", nullable = false)
	@Setter
	private DayOfWeek diaSemana;

	@Column(name = "hora_inicio", nullable = false)
	@Setter
	private LocalTime horaInicio;

	@Column(name = "hora_fin", nullable = false)
	@Setter
	private LocalTime horaFin;

	public HorarioSemanal(Medico medico, DayOfWeek diaSemana, LocalTime horaInicio, LocalTime horaFin) {
		this.medico = medico;
		this.diaSemana = diaSemana;
		this.horaInicio = horaInicio;
		this.horaFin = horaFin;
	}
}
