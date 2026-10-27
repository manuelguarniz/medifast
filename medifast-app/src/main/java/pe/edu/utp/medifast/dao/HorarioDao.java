package pe.edu.utp.medifast.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.utp.medifast.model.Horario;

import java.time.LocalDate;
import java.util.List;

/** DAO de {@link Horario}: bloques de 30 min generados desde la plantilla semanal (RN-08). */
public interface HorarioDao extends JpaRepository<Horario, Long> {

  List<Horario> findAllByMedicoIdAndFechaBetween(Long medicoId, LocalDate desde, LocalDate hasta);

  /**
   * Bloques del día que no están bloqueados ni ocupados por una cita activa (RF05, RN-01).
   *
   * @param idMedico médico
   * @param fecha día consultado
   * @return bloques libres ordenados por hora de inicio
   */
  @Query("""
      select h from Horario h
      where h.medico.id = :idMedico and h.fecha = :fecha and h.bloqueado = false
        and not exists (select 1 from CitaHorario ch where ch.horario = h and ch.activo = true)
      order by h.horaInicio""")
  List<Horario> bloquesLibres(@Param("idMedico") Long idMedico, @Param("fecha") LocalDate fecha);

  /**
   * Todos los bloques del día, libres u ocupados, para dibujar la grilla de P2.
   *
   * @param idMedico médico
   * @param fecha día consultado
   * @return bloques ordenados por hora de inicio
   */
  @Query("""
      select h from Horario h
      where h.medico.id = :idMedico and h.fecha = :fecha
      order by h.horaInicio
      """)
  List<Horario> bloquesDelDia(@Param("idMedico") Long idMedico, @Param("fecha") LocalDate fecha);
}
