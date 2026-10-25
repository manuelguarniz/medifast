package pe.edu.utp.medifast.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import pe.edu.utp.medifast.model.enums.EstadoPagoEnum;

/**
 * Pago simulado con Yape; se crea al adjuntar la constancia (tabla {@code pago}, RN-07, RN-11).
 */
@Entity
@Table(name = "pago")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pago {

	public static final String METODO_YAPE_SIMULADO = "YAPE_SIMULADO";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_pago")
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_cita", nullable = false, unique = true, updatable = false)
	private Cita cita;

	@Column(name = "metodo", nullable = false, updatable = false, length = 20)
	private String metodo = METODO_YAPE_SIMULADO;

	@Column(name = "monto", nullable = false, updatable = false, precision = 10, scale = 2)
	private BigDecimal monto;

	@Column(name = "nro_operacion", length = 30)
	private String nroOperacion;

	@Column(name = "comprobante_ruta", nullable = false, length = 255)
	private String comprobanteRuta;

	/** MIME de la constancia: image/jpeg, image/png o application/pdf (RN-11). */
	@Column(name = "comprobante_tipo", nullable = false, length = 50)
	private String comprobanteTipo;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false)
	@Setter
	private EstadoPagoEnum estado = EstadoPagoEnum.POR_VALIDAR;

	@Column(name = "fecha_pago", nullable = false, updatable = false)
	private LocalDateTime fechaPago;

	@Column(name = "validado_en")
	@Setter
	private LocalDateTime validadoEn;

	public Pago(Cita cita, BigDecimal monto, String nroOperacion, String comprobanteRuta, String comprobanteTipo,
			LocalDateTime fechaPago) {
		this.cita = cita;
		this.monto = monto;
		this.nroOperacion = nroOperacion;
		this.comprobanteRuta = comprobanteRuta;
		this.comprobanteTipo = comprobanteTipo;
		this.fechaPago = fechaPago;
	}
}
