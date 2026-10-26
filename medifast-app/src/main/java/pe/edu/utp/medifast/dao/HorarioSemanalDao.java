package pe.edu.utp.medifast.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.utp.medifast.model.HorarioSemanal;

/** DAO de {@link HorarioSemanal}. */
public interface HorarioSemanalDao extends JpaRepository<HorarioSemanal, Long> {

	List<HorarioSemanal> findByMedicoId(Long medicoId);
}
