package pe.edu.utp.medifast.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.utp.medifast.model.Horario;

/** DAO de {@link Horario}. */
public interface HorarioDao extends JpaRepository<Horario, Long> {
}
