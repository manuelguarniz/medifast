-- =============================================================================
-- MEDIFAST — 09_correccion_utf8.sql
-- Repara textos con doble codificación ("RodrÃ­guez" en vez de "Rodríguez").
--
-- Causa: 06_citas_historicas.sql y 07_citas_volumen.sql se ejecutaron con
-- `docker exec -i medifast-mysql mysql ...` sin --default-character-set. El
-- cliente del contenedor no tiene locale y usa latin1: leyó los bytes UTF-8 del
-- archivo como latin1 y MySQL los volvió a guardar como UTF-8 (í = C3 AD →
-- "Ã­" = C3 83 C2 AD). Desde esta versión todos los scripts fijan
-- SET NAMES utf8mb4, así que no se repite.
--
-- Corrección: deshace un nivel de codificación (utf8mb4 → bytes latin1 →
-- utf8mb4) solo en las filas que contienen "Ã" o "Â", los marcadores de la
-- doble codificación del español. Las filas correctas no se tocan y se
-- conserva actualizado_en.
--
-- Uso (con root):
--   mysql -u root -p < docs/sql/09_correccion_utf8.sql
--   docker exec -i medifast-mysql mysql -uroot -proot < docs/sql/09_correccion_utf8.sql
--
-- El script es re-ejecutable: tras corregir, ninguna fila vuelve a coincidir.
-- =============================================================================

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

USE medifast;

START TRANSACTION;

UPDATE paciente
   SET nombres        = CONVERT(BINARY CONVERT(nombres USING latin1) USING utf8mb4),
       actualizado_en = actualizado_en
 WHERE nombres COLLATE utf8mb4_bin LIKE '%Ã%' OR nombres COLLATE utf8mb4_bin LIKE '%Â%';

UPDATE paciente
   SET apellidos      = CONVERT(BINARY CONVERT(apellidos USING latin1) USING utf8mb4),
       actualizado_en = actualizado_en
 WHERE apellidos COLLATE utf8mb4_bin LIKE '%Ã%' OR apellidos COLLATE utf8mb4_bin LIKE '%Â%';

UPDATE medico
   SET nombres        = CONVERT(BINARY CONVERT(nombres USING latin1) USING utf8mb4),
       apellidos      = CONVERT(BINARY CONVERT(apellidos USING latin1) USING utf8mb4),
       especialidad   = CONVERT(BINARY CONVERT(especialidad USING latin1) USING utf8mb4),
       actualizado_en = actualizado_en
 WHERE CONCAT(nombres, apellidos, especialidad) COLLATE utf8mb4_bin LIKE '%Ã%'
    OR CONCAT(nombres, apellidos, especialidad) COLLATE utf8mb4_bin LIKE '%Â%';

UPDATE consultorio
   SET nombre         = CONVERT(BINARY CONVERT(nombre USING latin1) USING utf8mb4),
       direccion      = CONVERT(BINARY CONVERT(direccion USING latin1) USING utf8mb4),
       actualizado_en = actualizado_en
 WHERE CONCAT(nombre, IFNULL(direccion, '')) COLLATE utf8mb4_bin LIKE '%Ã%'
    OR CONCAT(nombre, IFNULL(direccion, '')) COLLATE utf8mb4_bin LIKE '%Â%';

UPDATE cita
   SET observacion    = CONVERT(BINARY CONVERT(observacion USING latin1) USING utf8mb4),
       actualizado_en = actualizado_en
 WHERE observacion COLLATE utf8mb4_bin LIKE '%Ã%' OR observacion COLLATE utf8mb4_bin LIKE '%Â%';

COMMIT;

-- Verificación: todo en 0
SELECT 'paciente' AS tabla, COUNT(*) AS filas_con_doble_codificacion
  FROM paciente
 WHERE CONCAT(nombres, apellidos) COLLATE utf8mb4_bin LIKE '%Ã%'
    OR CONCAT(nombres, apellidos) COLLATE utf8mb4_bin LIKE '%Â%'
UNION ALL
SELECT 'cita', COUNT(*)
  FROM cita
 WHERE observacion COLLATE utf8mb4_bin LIKE '%Ã%' OR observacion COLLATE utf8mb4_bin LIKE '%Â%';
