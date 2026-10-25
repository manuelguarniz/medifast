package pe.edu.utp.medifast.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.utp.medifast.model.HorarioSemanal;

/** DAO de {@link HorarioSemanal}. */
public interface HorarioSemanalDao extends JpaRepository<HorarioSemanal, Long> {
}
