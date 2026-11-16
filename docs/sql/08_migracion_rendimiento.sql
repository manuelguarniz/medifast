-- =============================================================================
-- MEDIFAST — 08_migracion_rendimiento.sql
-- Rendimiento con volumen (cientos de miles de citas):
--   1. cita guarda fecha, hora_inicio y hora_fin. Antes v_cita las calculaba
--      con MIN/MAX de sus bloques en una tabla derivada que MySQL materializaba
--      entera en cada consulta (0,2–0,3 s con 100 000 citas, aunque se pidiera
--      una sola). Se fijan al reservar y no cambian: los bloques de una cita no
--      se mueven, solo se liberan.
--   2. Índices para agenda y reportes (id_medico, fecha), historial del paciente
--      (id_paciente, fecha) y reporte de pagos (estado, validado_en).
--   3. v_cita lee las columnas nuevas: sin tabla derivada, MySQL la fusiona con
--      la consulta y usa los índices.
--
-- Solo para bases creadas con una versión anterior de 01_schema.sql; una base
-- nueva ya trae las columnas y los índices, y este script no le cambia nada.
--
-- Uso (con root: medifast_app no tiene permisos de ALTER ni CREATE ROUTINE):
--   mysql -u root -p < docs/sql/08_migracion_rendimiento.sql
--   docker exec -i medifast-mysql mysql -uroot -proot < docs/sql/08_migracion_rendimiento.sql
--
-- El script es re-ejecutable.
-- =============================================================================

USE medifast;

DROP PROCEDURE IF EXISTS ejecutar_si;

DELIMITER $$

-- Ejecuta p_ddl si p_falta es TRUE (columna o índice que aún no existe).
CREATE PROCEDURE ejecutar_si(IN p_falta BOOLEAN, IN p_ddl VARCHAR(1000))
BEGIN
    IF p_falta THEN
        SET @ddl = p_ddl;
        PREPARE sentencia FROM @ddl;
        EXECUTE sentencia;
        DEALLOCATE PREPARE sentencia;
    END IF;
END$$

DELIMITER ;

-- -----------------------------------------------------------------------------
-- 1. Fecha y horas de la cita
-- -----------------------------------------------------------------------------
SET @falta_fecha = NOT EXISTS (SELECT 1
                               FROM information_schema.COLUMNS
                               WHERE TABLE_SCHEMA = DATABASE()
                                 AND TABLE_NAME = 'cita'
                                 AND COLUMN_NAME = 'fecha');

CALL ejecutar_si(@falta_fecha,
                 'ALTER TABLE cita
                      ADD COLUMN fecha       DATE NULL AFTER cancelada_por,
                      ADD COLUMN hora_inicio TIME NULL AFTER fecha,
                      ADD COLUMN hora_fin    TIME NULL AFTER hora_inicio');

-- Datos existentes: desde sus bloques, como los calculaba v_cita. Se conserva
-- actualizado_en (la columna tiene ON UPDATE CURRENT_TIMESTAMP).
UPDATE cita c
    JOIN (SELECT ch.id_cita,
                 MIN(h.fecha)       AS fecha,
                 MIN(h.hora_inicio) AS hora_inicio,
                 MAX(h.hora_fin)    AS hora_fin
          FROM cita_horario ch
                   JOIN horario h ON h.id_horario = ch.id_horario
          GROUP BY ch.id_cita) b ON b.id_cita = c.id_cita
SET c.fecha          = b.fecha,
    c.hora_inicio    = b.hora_inicio,
    c.hora_fin       = b.hora_fin,
    c.actualizado_en = c.actualizado_en
WHERE c.fecha IS NULL;

CALL ejecutar_si(@falta_fecha,
                 'ALTER TABLE cita
                      MODIFY COLUMN fecha       DATE NOT NULL,
                      MODIFY COLUMN hora_inicio TIME NOT NULL,
                      MODIFY COLUMN hora_fin    TIME NOT NULL,
                      ADD CONSTRAINT ck_cita_horas CHECK (hora_fin > hora_inicio)');

-- -----------------------------------------------------------------------------
-- 2. Índices
-- -----------------------------------------------------------------------------
CALL ejecutar_si(NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cita'
                               AND INDEX_NAME = 'idx_cita_medico_fecha'),
                 'ALTER TABLE cita ADD INDEX idx_cita_medico_fecha (id_medico, fecha, hora_inicio)');

CALL ejecutar_si(NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cita'
                               AND INDEX_NAME = 'idx_cita_paciente_fecha'),
                 'ALTER TABLE cita ADD INDEX idx_cita_paciente_fecha (id_paciente, fecha, hora_inicio)');

CALL ejecutar_si(NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pago'
                               AND INDEX_NAME = 'idx_pago_estado_validado'),
                 'ALTER TABLE pago ADD INDEX idx_pago_estado_validado (estado, validado_en)');

DROP PROCEDURE ejecutar_si;

-- -----------------------------------------------------------------------------
-- 3. v_cita sin tabla derivada
-- -----------------------------------------------------------------------------
CREATE OR REPLACE VIEW v_cita AS
SELECT c.id_cita,
       c.id_paciente,
       CONCAT(p.nombres, ' ', p.apellidos) AS paciente,
       up.tipo_documento,
       up.numero_documento,
       c.id_medico,
       CONCAT(m.nombres, ' ', m.apellidos) AS medico,
       c.tipo_cita,
       c.estado,
       c.fecha,
       c.hora_inicio,
       c.hora_fin,
       pg.id_pago,
       pg.estado                          AS estado_pago,
       pg.monto,
       c.expira_en,
       c.observacion,
       c.cancelada_por,
       c.creado_en
FROM cita c
         JOIN paciente p ON p.id_paciente = c.id_paciente
         JOIN usuario up ON up.id_usuario = p.id_usuario
         JOIN medico m ON m.id_medico = c.id_medico
         LEFT JOIN pago pg ON pg.id_cita = c.id_cita;

ANALYZE TABLE cita, pago, cita_horario, horario;

-- -----------------------------------------------------------------------------
-- 4. Verificación: ninguna cita sin fecha y cada una coincide con sus bloques
-- -----------------------------------------------------------------------------
SELECT COUNT(*) AS citas_que_no_coinciden_con_sus_bloques
FROM cita c
         JOIN (SELECT ch.id_cita,
                      MIN(h.fecha)       AS fecha,
                      MIN(h.hora_inicio) AS hora_inicio,
                      MAX(h.hora_fin)    AS hora_fin
               FROM cita_horario ch
                        JOIN horario h ON h.id_horario = ch.id_horario
               GROUP BY ch.id_cita) b ON b.id_cita = c.id_cita
WHERE (c.fecha, c.hora_inicio, c.hora_fin) <> (b.fecha, b.hora_inicio, b.hora_fin);
