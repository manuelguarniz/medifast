package pe.edu.utp.medifast.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.utp.medifast.model.Pago;

/** DAO de {@link Pago}. */
public interface PagoDao extends JpaRepository<Pago, Long> {
}
