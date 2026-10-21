package pe.edu.utp.medifast.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.utp.medifast.model.Usuario;

/** DAO de {@link Usuario}. */
public interface UsuarioDao extends JpaRepository<Usuario, Long> {

	Optional<Usuario> findByNumeroDocumento(String numeroDocumento);

	boolean existsByNumeroDocumento(String numeroDocumento);
}
