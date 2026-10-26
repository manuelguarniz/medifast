package pe.edu.utp.medifast.dao;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.medifast.model.Tarifa;
import pe.edu.utp.medifast.model.enums.TipoCitaEnum;
/** DAO de {@link Tarifa}. */
public interface TarifaDao extends JpaRepository<Tarifa, Long> {
    Optional<Tarifa> findByMedicoIdAndTipoCita(Long idMedico, TipoCitaEnum tipoCita);
}