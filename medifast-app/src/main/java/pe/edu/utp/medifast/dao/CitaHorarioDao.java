package pe.edu.utp.medifast.dao;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.edu.utp.medifast.model.CitaHorario;
import pe.edu.utp.medifast.model.CitaHorarioId;

/** DAO de {@link CitaHorario}. */
public interface CitaHorarioDao extends JpaRepository<CitaHorario, CitaHorarioId> {

	@Query("""
			select ch.horario.id from CitaHorario ch
			where ch.horario.medico.id = :idMedico and ch.horario.fecha = :fecha and ch.activo = true
			""")
	List<Long> findIdsActivosByMedicoAndFecha(@Param("idMedico") Long idMedico,
			@Param("fecha") LocalDate fecha);
}
