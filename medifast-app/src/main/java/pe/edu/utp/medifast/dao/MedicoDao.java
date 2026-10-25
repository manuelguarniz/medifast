package pe.edu.utp.medifast.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.utp.medifast.model.Medico;

public interface MedicoDao extends JpaRepository<Medico, Long> {
    Optional<Medico> findByUsuarioNumeroDocumento(String numeroDocumento);
}
