package pe.edu.utp.medifast.model;

import java.math.BigDecimal;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import pe.edu.utp.medifast.model.enums.TipoCitaEnum;

/**
 * Duración y precio de cada tipo de cita por médico (tabla {@code tarifa}, RN-03).
 */
@Entity
@Table(name = "tarifa", uniqueConstraints = @UniqueConstraint(name = "uq_tarifa_medico_tipo",
		columnNames = { "id_medico", "tipo_cita" }))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tarifa extends EntidadAuditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_tarifa")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_medico", nullable = false, updatable = false)
	private Medico medico;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_cita", nullable = false, updatable = false)
	private TipoCitaEnum tipoCita;

	@JdbcTypeCode(SqlTypes.SMALLINT)
	@Column(name = "duracion_min", nullable = false)
	@Setter
	private int duracionMin;

	@Column(name = "precio", nullable = false, precision = 10, scale = 2)
	@Setter
	private BigDecimal precio;

	public Tarifa(Medico medico, TipoCitaEnum tipoCita, int duracionMin, BigDecimal precio) {
		this.medico = medico;
		this.tipoCita = tipoCita;
		this.duracionMin = duracionMin;
		this.precio = precio;
	}
}
