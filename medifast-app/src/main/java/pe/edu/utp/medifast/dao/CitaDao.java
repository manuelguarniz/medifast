package pe.edu.utp.medifast.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import pe.edu.utp.medifast.dto.ConteoEstadoDto;
import pe.edu.utp.medifast.model.Cita;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;

/** DAO de {@link Cita}. */
public interface CitaDao extends JpaRepository<Cita, Long> {

	@EntityGraph(attributePaths = { "paciente", "medico", "bloques", "bloques.horario" })
	@Override
	Optional<Cita> findById(Long id);

	/**
	 * Cita con bloqueo de escritura ({@code SELECT … FOR UPDATE}) para adjuntar la constancia: el evento de RF16
	 * espera a que termine la transacción y no cancela una cita que se está pagando (RNF05).
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from Cita c join fetch c.paciente where c.id = :id")
	Optional<Cita> findParaPagarById(@Param("id") Long id);

	boolean existsByPacienteIdAndMedicoIdAndEstado(Long idPaciente, Long idMedico, EstadoCitaEnum estado);

	@EntityGraph(attributePaths = { "paciente", "medico", "bloques", "bloques.horario" })
	Optional<Cita> findFirstByPacienteIdAndEstadoIn(Long idPaciente, List<EstadoCitaEnum> estados);

	/**
	 * Citas del médico por estado en un rango (indicadores y total de P9). Lee {@code cita} directamente, sin los
	 * joins de {@code v_cita}, con el índice {@code idx_cita_medico_fecha}.
	 *
	 * @param idMedico médico autenticado
	 * @param desde primer día, inclusivo
	 * @param hasta último día, inclusivo
	 * @param estado estado de la cita; {@code null} = todos
	 * @return una fila por estado con citas
	 */
	@Query("""
			select new pe.edu.utp.medifast.dto.ConteoEstadoDto(c.estado, count(c))
			from Cita c
			where c.medico.id = :idMedico
			  and c.fecha between :desde and :hasta
			  and (:estado is null or c.estado = :estado)
			group by c.estado
			""")
	List<ConteoEstadoDto> contarPorEstado(@Param("idMedico") Long idMedico, @Param("desde") LocalDate desde,
			@Param("hasta") LocalDate hasta, @Param("estado") EstadoCitaEnum estado);

	/** Citas del paciente sin contar {@code idExcluida} (tamaño del historial de P5). */
	long countByPacienteIdAndIdNot(Long idPaciente, Long idExcluida);
}
