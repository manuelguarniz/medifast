package pe.edu.utp.medifast.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import pe.edu.utp.medifast.model.enums.RolEnum;
import pe.edu.utp.medifast.model.enums.TipoDocumentoEnum;

/**
 * Credenciales de acceso de pacientes y médicos (tabla {@code usuario}, RN-12).
 */
@Entity
@Table(name = "usuario")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario extends EntidadAuditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_usuario")
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_documento", nullable = false)
	private TipoDocumentoEnum tipoDocumento;

	@Column(name = "numero_documento", nullable = false, unique = true, updatable = false, length = 12)
	private String numeroDocumento;

	@Column(name = "contrasena_hash", nullable = false, length = 60)
	@Setter
	private String contrasenaHash;

	@Enumerated(EnumType.STRING)
	@Column(name = "rol", nullable = false)
	private RolEnum rol;

	@Column(name = "activo", nullable = false)
	@Setter
	private boolean activo = true;

	public Usuario(TipoDocumentoEnum tipoDocumento, String numeroDocumento, String contrasenaHash, RolEnum rol) {
		this.tipoDocumento = tipoDocumento;
		this.numeroDocumento = numeroDocumento;
		this.contrasenaHash = contrasenaHash;
		this.rol = rol;
	}
}
