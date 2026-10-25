package pe.edu.utp.medifast.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Bloque de {@link Horario} que ocupa una {@link Cita}: 1 para CONTROL, 2 para NUEVO (tabla {@code cita_horario},
 * RN-01). Al liberar la cita se marca {@code activo = false} y el bloque vuelve a quedar disponible.
 * <p>
 * La columna generada {@code horario_activo} (RN-01) no se mapea: solo existe para el índice único en MySQL.
 */
@Entity
@Table(name = "cita_horario")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CitaHorario {

	@EmbeddedId
	private CitaHorarioId id = new CitaHorarioId();

	@MapsId("idCita")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_cita")
	private Cita cita;

	@MapsId("idHorario")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_horario")
	private Horario horario;

	@Column(name = "activo", nullable = false)
	@Setter
	private boolean activo = true;

	CitaHorario(Cita cita, Horario horario) {
		this.cita = cita;
		this.horario = horario;
	}
}
