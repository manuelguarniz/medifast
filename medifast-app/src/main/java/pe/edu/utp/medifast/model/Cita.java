package pe.edu.utp.medifast.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
 * Reserva de un paciente con un médico (tabla {@code cita}, RN-02, RN-05, RN-09). La fecha y las horas son el
 * inicio y el fin de sus {@link #getBloques() bloques}: se fijan al agregarlos y se guardan para que {@link VistaCita}
 * no tenga que calcularlas.
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

	@Column(name = "fecha", nullable = false, updatable = false)
	private LocalDate fecha;

	@Column(name = "hora_inicio", nullable = false, updatable = false)
	private LocalTime horaInicio;

	@Column(name = "hora_fin", nullable = false, updatable = false)
	private LocalTime horaFin;

	@OneToMany(mappedBy = "cita", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	private List<CitaHorario> bloques = new ArrayList<>();

	public Cita(Paciente paciente, Medico medico, TipoCitaEnum tipoCita, LocalDateTime expiraEn) {
		this.paciente = paciente;
		this.medico = medico;
		this.tipoCita = tipoCita;
		this.expiraEn = expiraEn;
	}

	/** Reserva un bloque para esta cita (RN-01) y amplía su inicio y fin hasta cubrirlo. */
	public void agregarBloque(Horario horario) {
		if (bloques.isEmpty()) {
			fecha = horario.getFecha();
			horaInicio = horario.getHoraInicio();
			horaFin = horario.getHoraFin();
		} else {
			horaInicio = horario.getHoraInicio().isBefore(horaInicio) ? horario.getHoraInicio() : horaInicio;
			horaFin = horario.getHoraFin().isAfter(horaFin) ? horario.getHoraFin() : horaFin;
		}
		bloques.add(new CitaHorario(this, horario));
	}

	public List<CitaHorario> getBloques() {
		return Collections.unmodifiableList(bloques);
	}
}
