-- =============================================================================
-- MEDIFAST — 05_evento_vencimiento.sql
-- RF16 / RN-04 (SD-04): cada minuto cancela las reservas PENDIENTE_PAGO cuyo
-- plazo de 15 min venció y libera sus bloques. Corre dentro de MySQL (Event
-- Scheduler), aunque la aplicación esté detenida.
--
-- Uso (con root: medifast_app no tiene el privilegio EVENT):
--   mysql -u root -p < docs/sql/05_evento_vencimiento.sql
--   docker exec -i medifast-mysql mysql -uroot -proot < docs/sql/05_evento_vencimiento.sql
--
-- El script es re-ejecutable: vuelve a crear el evento. Requiere
-- event_scheduler = ON (valor por defecto en MySQL 8.4; docker-compose.yml lo
-- fija con --event-scheduler=ON). Verificación:
--   SHOW VARIABLES LIKE 'event_scheduler';
--   SHOW EVENTS FROM medifast;
-- =============================================================================

USE medifast;

DROP EVENT IF EXISTS ev_vencer_reservas;

DELIMITER $$

CREATE EVENT ev_vencer_reservas
    ON SCHEDULE EVERY 1 MINUTE
    STARTS CURRENT_TIMESTAMP
    ON COMPLETION PRESERVE
    ENABLE
    COMMENT 'RF16: cancela las reservas PENDIENTE_PAGO vencidas y libera sus bloques'
    DO
    BEGIN
        -- Un solo corte para las dos sentencias: liberan y cancelan las mismas citas.
        DECLARE v_corte DATETIME DEFAULT NOW();

        -- Si algo falla, no queda una cita cancelada con sus bloques aún ocupados.
        DECLARE EXIT HANDLER FOR SQLEXCEPTION
            BEGIN
                ROLLBACK;
                RESIGNAL;
            END;

        START TRANSACTION;

        -- 1. Libera los bloques (RN-01: horario_activo pasa a NULL). El JOIN lee la
        --    cita con bloqueo, así que espera a una constancia que se esté
        --    adjuntando (PagoService bloquea la cita con SELECT … FOR UPDATE) y, si
        --    esa cita ya pasó a PROGRAMADA, no la toca.
        UPDATE cita_horario ch
            JOIN cita c ON c.id_cita = ch.id_cita
        SET ch.activo = FALSE
        WHERE c.estado = 'PENDIENTE_PAGO'
          AND c.expira_en < v_corte
          AND ch.activo = TRUE;

        -- 2. Cancela la cita con la observación literal de RF16 (RN-09:
        --    paciente_activo pasa a NULL y el paciente puede volver a reservar).
        UPDATE cita
        SET estado        = 'CANCELADA',
            cancelada_por = 'SISTEMA',
            observacion   = 'Tiempo de reserva vencido'
        WHERE estado = 'PENDIENTE_PAGO'
          AND expira_en < v_corte;

        COMMIT;
    END$$

DELIMITER ;
