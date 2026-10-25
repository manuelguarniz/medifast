package pe.edu.utp.medifast.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.utp.medifast.model.Cita;

/** DAO de {@link Cita}. */
public interface CitaDao extends JpaRepository<Cita, Long> {
}
