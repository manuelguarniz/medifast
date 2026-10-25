package pe.edu.utp.medifast.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.utp.medifast.model.Consultorio;

/** DAO de {@link Consultorio}. */
public interface ConsultorioDao extends JpaRepository<Consultorio, Long> {
}
