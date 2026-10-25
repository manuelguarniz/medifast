package pe.edu.utp.medifast.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.utp.medifast.model.Medico;

/** DAO de {@link Medico}. */
public interface MedicoDao extends JpaRepository<Medico, Long> {
}
