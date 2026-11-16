package pe.edu.utp.medifast.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.edu.utp.medifast.dto.PagoValidadoDto;
import pe.edu.utp.medifast.dto.TotalPagosDto;
import pe.edu.utp.medifast.model.Pago;

/** DAO de {@link Pago}. */
public interface PagoDao extends JpaRepository<Pago, Long> {

    String PAGOS_VALIDADOS = """
            SELECT new pe.edu.utp.medifast.dto.PagoValidadoDto(
                       p.validadoEn, c.id, CONCAT(pa.nombres, ' ', pa.apellidos), u.tipoDocumento,
                       u.numeroDocumento, c.tipoCita, p.nroOperacion, p.monto)
            FROM Pago p
            JOIN p.cita c
            JOIN c.paciente pa
            JOIN pa.usuario u
            WHERE c.medico.id = :idMedico
              AND p.estado = pe.edu.utp.medifast.model.enums.EstadoPagoEnum.VALIDADO
              AND p.validadoEn >= :desde
              AND p.validadoEn < :hastaExclusivo
            ORDER BY p.validadoEn ASC, c.id ASC
            """;

    /**
     * Pagos validados de un médico en un período, por fecha de validación (reporte P10, RF09). Es la consulta
     * documentada en {@code docs/sql/03_consultas.sql} (sección 4). Usa el índice {@code idx_pago_estado_validado}.
     *
     * @param idMedico médico autenticado
     * @param desde inicio del período, inclusivo
     * @param hastaExclusivo fin del período, exclusivo
     * @return un pago por fila, del más antiguo al más reciente
     */
    @Query(PAGOS_VALIDADOS)
    List<PagoValidadoDto> pagosValidados(@Param("idMedico") Long idMedico, @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo);

    /** Una página de {@link #pagosValidados(Long, LocalDateTime, LocalDateTime)}, con {@code LIMIT}/{@code OFFSET}. */
    @Query(PAGOS_VALIDADOS)
    List<PagoValidadoDto> pagosValidados(@Param("idMedico") Long idMedico, @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo, Pageable pagina);

    /** Cantidad y suma de {@link #pagosValidados(Long, LocalDateTime, LocalDateTime)} (indicadores de P10). */
    @Query("""
            SELECT new pe.edu.utp.medifast.dto.TotalPagosDto(COUNT(p), SUM(p.monto))
            FROM Pago p
            JOIN p.cita c
            WHERE c.medico.id = :idMedico
              AND p.estado = pe.edu.utp.medifast.model.enums.EstadoPagoEnum.VALIDADO
              AND p.validadoEn >= :desde
              AND p.validadoEn < :hastaExclusivo
            """)
    TotalPagosDto totalPagosValidados(@Param("idMedico") Long idMedico, @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo);

    Optional<Pago> findByCitaId(Long idCita);
}
