package pe.edu.utp.medifast.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import pe.edu.utp.medifast.model.enums.CanceladaPorEnum;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;
import pe.edu.utp.medifast.model.enums.EstadoPagoEnum;
import pe.edu.utp.medifast.model.enums.TipoCitaEnum;
import pe.edu.utp.medifast.model.enums.TipoDocumentoEnum;

/**
 * Vista {@code v_cita} (solo lectura): cita con paciente, médico, inicio/fin calculados desde sus bloques y pago.
 * Alimenta agendas, historial y reportes.
 */
@Entity
@Immutable
@Table(name = "v_cita")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VistaCita {

	@Id
	@Column(name = "id_cita")
	private Long idCita;

	@Column(name = "id_paciente")
	private Long idPaciente;

	@Column(name = "paciente")
	private String paciente;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_documento")
	private TipoDocumentoEnum tipoDocumento;

	@Column(name = "numero_documento")
	private String numeroDocumento;

	@Column(name = "id_medico")
	private Long idMedico;

	@Column(name = "medico")
	private String medico;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_cita")
	private TipoCitaEnum tipoCita;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado")
	private EstadoCitaEnum estado;

	@Column(name = "fecha")
	private LocalDate fecha;

	@Column(name = "hora_inicio")
	private LocalTime horaInicio;

	@Column(name = "hora_fin")
	private LocalTime horaFin;

	@Column(name = "id_pago")
	private Long idPago;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado_pago")
	private EstadoPagoEnum estadoPago;

	@Column(name = "monto")
	private BigDecimal monto;

	@Column(name = "expira_en")
	private LocalDateTime expiraEn;

	@Column(name = "observacion")
	private String observacion;

	@Enumerated(EnumType.STRING)
	@Column(name = "cancelada_por")
	private CanceladaPorEnum canceladaPor;

	@Column(name = "creado_en")
	private LocalDateTime creadoEn;
}
