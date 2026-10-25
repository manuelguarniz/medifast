package pe.edu.utp.medifast.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.utp.medifast.model.CitaHorario;
import pe.edu.utp.medifast.model.CitaHorarioId;

/** DAO de {@link CitaHorario}. */
public interface CitaHorarioDao extends JpaRepository<CitaHorario, CitaHorarioId> {
}
