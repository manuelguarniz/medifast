package pe.edu.utp.medifast.dao;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;

/**
 * DAO de {@link VistaCita}.
 */
public interface VistaCitaDao extends JpaRepository<VistaCita, Long> {

    @Query("""
            SELECT v
            FROM VistaCita v
            WHERE v.idMedico = :idMedico
              AND v.fecha BETWEEN :desde AND :hasta
              AND (:estado IS NULL OR v.estado = :estado)
            ORDER BY v.fecha ASC, v.horaInicio ASC
            """)
    List<VistaCita> reporteCitas(@Param("idMedico") Long idMedico, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta, @Param("estado") EstadoCitaEnum estado);

    /** Agenda del médico (P7, RF04): citas del período ordenadas por fecha y hora. */
    List<VistaCita> findAllByIdMedicoAndFechaBetweenOrderByFechaAscHoraInicioAsc(Long idMedico, LocalDate desde,
            LocalDate hasta);

    /** Historial del paciente (P5, RF10): de la más reciente a la más antigua. */
    List<VistaCita> findAllByIdPacienteOrderByFechaDescHoraInicioDesc(Long idPaciente);
}
