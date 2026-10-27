package pe.edu.utp.medifast.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.utp.medifast.model.Paciente;

/** DAO de {@link Paciente}. */
public interface PacienteDao extends JpaRepository<Paciente, Long> {

	/** Carga también el usuario, que "Mi perfil" muestra fuera de la transacción (open-in-view desactivado). */
	@EntityGraph(attributePaths = "usuario")
	Optional<Paciente> findByUsuarioNumeroDocumento(String numeroDocumento);

	boolean existsByCorreoIgnoreCase(String correo);

	boolean existsByCorreoIgnoreCaseAndIdNot(String correo, Long id);
}
