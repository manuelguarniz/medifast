package pe.edu.utp.medifast.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.utp.medifast.model.Cita;
import pe.edu.utp.medifast.model.enums.EstadoCitaEnum;

/** DAO de {@link Cita}. */
public interface CitaDao extends JpaRepository<Cita, Long> {

	@EntityGraph(attributePaths = { "paciente", "medico", "bloques", "bloques.horario" })
	@Override
	Optional<Cita> findById(Long id);

	boolean existsByPacienteIdAndMedicoIdAndEstado(Long idPaciente, Long idMedico, EstadoCitaEnum estado);

	@EntityGraph(attributePaths = { "paciente", "medico", "bloques", "bloques.horario" })
	Optional<Cita> findFirstByPacienteIdAndEstadoIn(Long idPaciente, List<EstadoCitaEnum> estados);
}
