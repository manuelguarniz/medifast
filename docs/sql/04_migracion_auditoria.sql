-- =============================================================================
-- MEDIFAST — 04_migracion_auditoria.sql
-- Agrega las columnas de auditoría (RNF13) a las tablas que no las tenían:
--   consultorio, medico, tarifa, horario_semanal, horario, cita_horario, pago
--   creado_en       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
--   actualizado_en  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
--
-- Solo para bases creadas con una versión anterior de 01_schema.sql; una base
-- nueva ya trae las columnas y este script no le cambia nada.
--
-- Uso (con root: medifast_app no tiene permisos de ALTER ni CREATE ROUTINE):
--   mysql -u root -p < docs/sql/04_migracion_auditoria.sql
--   docker exec -i medifast-mysql mysql -uroot -proot < docs/sql/04_migracion_auditoria.sql
--
-- El script es re-ejecutable: solo agrega las columnas que faltan y solo
-- rellena los datos históricos de las tablas que acaba de modificar.
-- =============================================================================

USE medifast;

DROP PROCEDURE IF EXISTS agregar_auditoria;

DELIMITER $$

-- Agrega creado_en / actualizado_en a p_tabla si no existen.
-- p_agregada = TRUE cuando creado_en se agregó en esta ejecución.
CREATE PROCEDURE agregar_auditoria(IN p_tabla VARCHAR(64), OUT p_agregada BOOLEAN)
BEGIN
    SET p_agregada = FALSE;

    IF NOT EXISTS (SELECT 1
                   FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME = p_tabla
                     AND COLUMN_NAME = 'creado_en') THEN
        SET @ddl = CONCAT('ALTER TABLE ', p_tabla,
                          ' ADD COLUMN creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP');
        PREPARE sentencia FROM @ddl;
        EXECUTE sentencia;
        DEALLOCATE PREPARE sentencia;
        SET p_agregada = TRUE;
    END IF;

    IF NOT EXISTS (SELECT 1
                   FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME = p_tabla
                     AND COLUMN_NAME = 'actualizado_en') THEN
        SET @ddl = CONCAT('ALTER TABLE ', p_tabla,
                          ' ADD COLUMN actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP',
                          ' ON UPDATE CURRENT_TIMESTAMP');
        PREPARE sentencia FROM @ddl;
        EXECUTE sentencia;
        DEALLOCATE PREPARE sentencia;
    END IF;
END$$

DELIMITER ;

-- -----------------------------------------------------------------------------
-- 1. Columnas. Las filas existentes reciben la fecha de esta migración.
-- -----------------------------------------------------------------------------
CALL agregar_auditoria('consultorio', @consultorio);
CALL agregar_auditoria('medico', @medico);
CALL agregar_auditoria('tarifa', @tarifa);
CALL agregar_auditoria('horario_semanal', @horario_semanal);
CALL agregar_auditoria('horario', @horario);
CALL agregar_auditoria('cita_horario', @cita_horario);
CALL agregar_auditoria('pago', @pago);

-- -----------------------------------------------------------------------------
-- 2. Datos históricos: donde hay una fecha confiable se usa en lugar de la
--    fecha de la migración. Solo se ejecuta si la columna se acaba de crear.
-- -----------------------------------------------------------------------------

-- pago: se creó al adjuntar la constancia y cambió por última vez al validarse
UPDATE pago
SET creado_en      = fecha_pago,
    actualizado_en = COALESCE(validado_en, fecha_pago)
WHERE @pago;

-- cita_horario: el bloque se reservó junto con la cita; si se liberó, fue con
-- el último cambio de la cita
UPDATE cita_horario ch
    JOIN cita c ON c.id_cita = ch.id_cita
SET ch.creado_en      = c.creado_en,
    ch.actualizado_en = IF(ch.activo, c.creado_en, c.actualizado_en)
WHERE @cita_horario;

DROP PROCEDURE agregar_auditoria;

-- -----------------------------------------------------------------------------
-- 3. Verificación: las 10 tablas deben tener 2 columnas de auditoría.
-- -----------------------------------------------------------------------------
SELECT TABLE_NAME                     AS tabla,
       COUNT(*)                       AS columnas_auditoria
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND COLUMN_NAME IN ('creado_en', 'actualizado_en')
  AND TABLE_NAME IN (SELECT TABLE_NAME
                     FROM information_schema.TABLES
                     WHERE TABLE_SCHEMA = DATABASE()
                       AND TABLE_TYPE = 'BASE TABLE')
GROUP BY TABLE_NAME
ORDER BY TABLE_NAME;
