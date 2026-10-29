package pe.edu.utp.medifast.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.edu.utp.medifast.model.VistaCita;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;

/**
 * DAO de {@link VistaCita}.
 */
public interface VistaCitaDao extends JpaRepository<VistaCita, Long> {

    String REPORTE_CITAS = """
            SELECT v
            FROM VistaCita v
            WHERE v.idMedico = :idMedico
              AND v.fecha BETWEEN :desde AND :hasta
              AND (:estado IS NULL OR v.estado = :estado)
            ORDER BY v.fecha ASC, v.horaInicio ASC
            """;

    /** Reporte de citas (P9) completo, para el Excel. */
    @Query(REPORTE_CITAS)
    List<VistaCita> reporteCitas(@Param("idMedico") Long idMedico, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta, @Param("estado") EstadoCitaEnum estado);

    /** Una página del reporte de citas (P9), con {@code LIMIT}/{@code OFFSET}. */
    @Query(REPORTE_CITAS)
    List<VistaCita> reporteCitas(@Param("idMedico") Long idMedico, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta, @Param("estado") EstadoCitaEnum estado, Pageable pagina);

    /** Agenda del médico (P7, RF04): citas del período ordenadas por fecha y hora. */
    List<VistaCita> findAllByIdMedicoAndFechaBetweenOrderByFechaAscHoraInicioAsc(Long idMedico, LocalDate desde,
            LocalDate hasta);

    /** Cita activa del paciente (RN-09: como máximo una). */
    Optional<VistaCita> findFirstByIdPacienteAndEstadoIn(Long idPaciente, List<EstadoCitaEnum> estados);

    /**
     * Una página del historial del paciente (P5, RF10), de la más reciente a la más antigua, sin la cita
     * {@code idExcluida} (la que se muestra en "Próximas").
     */
    List<VistaCita> findAllByIdPacienteAndIdCitaNotOrderByFechaDescHoraInicioDesc(Long idPaciente, Long idExcluida,
            Pageable pagina);
}
