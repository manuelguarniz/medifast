package pe.edu.utp.medifast.dao;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.utp.medifast.model.Horario;

public interface HorarioDao extends JpaRepository<Horario, Long> {

    @Query("""
        select h from Horario h
        where h.medico.id = :idMedico and h.fecha = :fecha and h.bloqueado = false
          and not exists (select 1 from CitaHorario ch where ch.horario = h and ch.activo = true)
        order by h.horaInicio""")
    List<Horario> bloquesLibres(@Param("idMedico") Long idMedico, @Param("fecha") LocalDate fecha);
}
