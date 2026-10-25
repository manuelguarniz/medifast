package pe.edu.utp.medifast.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.utp.medifast.model.VistaCita;

/** DAO de {@link VistaCita}. */
public interface VistaCitaDao extends JpaRepository<VistaCita, Long> {
}
