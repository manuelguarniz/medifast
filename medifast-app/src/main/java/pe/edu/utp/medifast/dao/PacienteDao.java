package pe.edu.utp.medifast.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.utp.medifast.model.Paciente;

/** DAO de {@link Paciente}. */
public interface PacienteDao extends JpaRepository<Paciente, Long> {

	Optional<Paciente> findByUsuarioNumeroDocumento(String numeroDocumento);
}
