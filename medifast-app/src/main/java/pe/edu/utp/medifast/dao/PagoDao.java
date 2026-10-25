package pe.edu.utp.medifast.dao;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.edu.utp.medifast.model.Pago;

public interface PagoDao extends JpaRepository<Pago, Long>{
    @Query(value = """
            SELECT DATE(p.validado_en) AS dia,
                   COUNT(*) AS cantidad,
                   SUM(p.monto) AS total
            FROM pago p
            JOIN cita c ON c.id_cita = p.id_cita
            WHERE c.id_medico = :idMedico
              AND p.estado = 'VALIDADO'
              AND p.validado_en >= :desde
              AND p.validado_en < :hastaExclusivo
            GROUP BY DATE(p.validado_en)
            ORDER BY dia
            """, nativeQuery = true)
    List<Object[]> resumenValidados(@Param("idMedico") Long idMedico, @Param("desde") LocalDateTime desde, @Param("hastaExclusivo") LocalDateTime hastaExclusivo);
}
