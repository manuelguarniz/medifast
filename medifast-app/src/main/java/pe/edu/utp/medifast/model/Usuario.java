package pe.edu.utp.medifast.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Credenciales de acceso de pacientes y médicos (tabla {@code usuario}, RN-12).
 */
@Entity
@Table(name = "usuario")
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_usuario")
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_documento", nullable = false)
	private TipoDocumento tipoDocumento;

	@Column(name = "numero_documento", nullable = false, unique = true, updatable = false, length = 12)
	private String numeroDocumento;

	@Column(name = "contrasena_hash", nullable = false, length = 60)
	private String contrasenaHash;

	@Enumerated(EnumType.STRING)
	@Column(name = "rol", nullable = false)
	private Rol rol;

	@Column(name = "activo", nullable = false)
	private boolean activo = true;

	protected Usuario() {
	}

	public Usuario(TipoDocumento tipoDocumento, String numeroDocumento, String contrasenaHash, Rol rol) {
		this.tipoDocumento = tipoDocumento;
		this.numeroDocumento = numeroDocumento;
		this.contrasenaHash = contrasenaHash;
		this.rol = rol;
	}

	public Long getId() {
		return id;
	}

	public TipoDocumento getTipoDocumento() {
		return tipoDocumento;
	}

	public String getNumeroDocumento() {
		return numeroDocumento;
	}

	public String getContrasenaHash() {
		return contrasenaHash;
	}

	public void setContrasenaHash(String contrasenaHash) {
		this.contrasenaHash = contrasenaHash;
	}

	public Rol getRol() {
		return rol;
	}

	public boolean isActivo() {
		return activo;
	}

	public void setActivo(boolean activo) {
		this.activo = activo;
	}
}
