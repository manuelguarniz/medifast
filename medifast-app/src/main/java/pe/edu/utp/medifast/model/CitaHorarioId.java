package pe.edu.utp.medifast.model;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Clave compuesta de {@link CitaHorario}: {@code (id_cita, id_horario)}. */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CitaHorarioId implements Serializable {

	private static final long serialVersionUID = 1L;

	@Column(name = "id_cita")
	private Long idCita;

	@Column(name = "id_horario")
	private Long idHorario;
}
