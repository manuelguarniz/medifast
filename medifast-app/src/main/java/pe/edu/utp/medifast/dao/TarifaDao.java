package pe.edu.utp.medifast.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.utp.medifast.model.Tarifa;

/** DAO de {@link Tarifa}. */
public interface TarifaDao extends JpaRepository<Tarifa, Long> {
}
