package pe.edu.utp.medifast.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.utp.medifast.model.Paciente;

/** DAO de {@link Paciente}. */
public interface PacienteDao extends JpaRepository<Paciente, Long> {
}
