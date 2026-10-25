package pe.edu.utp.medifast.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import lombok.Getter;

/**
 * Columnas de auditoría comunes a todas las tablas (RNF13). Los valores los pone MySQL
 * ({@code DEFAULT CURRENT_TIMESTAMP} y {@code ON UPDATE CURRENT_TIMESTAMP}); Hibernate solo los lee.
 */
@MappedSuperclass
@Getter
public abstract class EntidadAuditable {

	@Generated(event = EventType.INSERT)
	@Column(name = "creado_en", insertable = false, updatable = false)
	private LocalDateTime creadoEn;

	@Generated(event = { EventType.INSERT, EventType.UPDATE })
	@Column(name = "actualizado_en", insertable = false, updatable = false)
	private LocalDateTime actualizadoEn;
}
