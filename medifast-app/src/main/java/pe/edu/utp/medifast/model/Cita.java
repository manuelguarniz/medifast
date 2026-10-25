package pe.edu.utp.medifast.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import pe.edu.utp.medifast.model.enums.CanceladaPorEnum;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;
import pe.edu.utp.medifast.model.enums.TipoCitaEnum;

/**
 * Reserva de un paciente con un médico (tabla {@code cita}, RN-02, RN-05, RN-09). La fecha y la hora no se
 * guardan aquí: salen de sus {@link #getBloques() bloques} (ver {@link VistaCita}).
 * <p>
 * La columna generada {@code paciente_activo} (RN-09) no se mapea: solo existe para el índice único en MySQL.
 */
@Entity
@Table(name = "cita")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cita extends EntidadAuditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_cita")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_paciente", nullable = false, updatable = false)
	private Paciente paciente;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_medico", nullable = false, updatable = false)
	private Medico medico;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_cita", nullable = false, updatable = false)
	private TipoCitaEnum tipoCita;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false)
	@Setter
	private EstadoCitaEnum estado = EstadoCitaEnum.PENDIENTE_PAGO;

	@Column(name = "expira_en")
	@Setter
	private LocalDateTime expiraEn;

	@Column(name = "observacion", length = 500)
	@Setter
	private String observacion;

	@Enumerated(EnumType.STRING)
	@Column(name = "cancelada_por")
	@Setter
	private CanceladaPorEnum canceladaPor;

	@OneToMany(mappedBy = "cita", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	private List<CitaHorario> bloques = new ArrayList<>();

	public Cita(Paciente paciente, Medico medico, TipoCitaEnum tipoCita, LocalDateTime expiraEn) {
		this.paciente = paciente;
		this.medico = medico;
		this.tipoCita = tipoCita;
		this.expiraEn = expiraEn;
	}

	/** Reserva un bloque para esta cita (RN-01). */
	public void agregarBloque(Horario horario) {
		bloques.add(new CitaHorario(this, horario));
	}

	public List<CitaHorario> getBloques() {
		return Collections.unmodifiableList(bloques);
	}
}
