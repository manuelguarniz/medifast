-- =============================================================================
-- MEDIFAST — 03_consultas.sql
-- Consultas a entregar (README §11). Ejecutar después de 01 y 02.
-- La sección 6 contiene INSERT que DEBEN FALLAR: ejecútalo con
--   mysql --force -u root -p medifast < docs/sql/03_consultas.sql
-- para que el cliente continúe tras cada error esperado.
-- =============================================================================

USE medifast;

-- -----------------------------------------------------------------------------
-- 1. Vista v_cita (definida en 01_schema.sql): cita + paciente + médico +
--    inicio/fin calculados desde sus bloques + estado del pago
-- -----------------------------------------------------------------------------
SELECT id_cita, paciente, medico, tipo_cita, estado, fecha, hora_inicio, hora_fin,
       estado_pago, monto
FROM v_cita
ORDER BY fecha, hora_inicio;

-- -----------------------------------------------------------------------------
-- 2. Verificación de todas las relaciones (JOIN de las 10 tablas)
-- -----------------------------------------------------------------------------
SELECT co.nombre                              AS consultorio,
       CONCAT(m.nombres, ' ', m.apellidos)    AS medico,
       um.numero_documento                    AS documento_medico,
       CONCAT(p.nombres, ' ', p.apellidos)    AS paciente,
       up.tipo_documento,
       up.numero_documento,
       c.id_cita,
       c.tipo_cita,
       t.duracion_min,
       t.precio                               AS precio_tarifa,
       c.estado,
       h.fecha,
       h.hora_inicio,
       h.hora_fin,
       ch.activo                              AS bloque_activo,
       hs.hora_inicio                         AS atencion_desde,
       hs.hora_fin                            AS atencion_hasta,
       pg.estado                              AS estado_pago,
       pg.monto
FROM cita c
         JOIN paciente p ON p.id_paciente = c.id_paciente
         JOIN usuario up ON up.id_usuario = p.id_usuario
         JOIN medico m ON m.id_medico = c.id_medico
         JOIN usuario um ON um.id_usuario = m.id_usuario
         JOIN consultorio co ON co.id_consultorio = m.id_consultorio
         JOIN tarifa t ON t.id_medico = m.id_medico AND t.tipo_cita = c.tipo_cita
         JOIN cita_horario ch ON ch.id_cita = c.id_cita
         JOIN horario h ON h.id_horario = ch.id_horario
         JOIN horario_semanal hs ON hs.id_medico = m.id_medico AND hs.dia_semana = WEEKDAY(h.fecha) + 1
         LEFT JOIN pago pg ON pg.id_cita = c.id_cita
ORDER BY h.fecha, h.hora_inicio;

-- -----------------------------------------------------------------------------
-- 3. Reporte 1 (RF09): citas por estado en un rango de fechas
--    Cambia @desde / @hasta para el día o la semana que se quiera consultar.
-- -----------------------------------------------------------------------------
SET @desde = CURDATE() - INTERVAL 30 DAY;
SET @hasta = CURDATE() + INTERVAL 30 DAY;

SELECT v.estado, COUNT(*) AS cantidad
FROM v_cita v
WHERE v.id_medico = 1
  AND v.fecha BETWEEN @desde AND @hasta
GROUP BY v.estado
ORDER BY FIELD(v.estado, 'PENDIENTE_PAGO', 'PROGRAMADA', 'ATENDIDA', 'NO_ASISTIO', 'CANCELADA');

-- Detalle por semana (YEARWEEK modo 3 = semana ISO, de lunes a domingo)
SELECT YEARWEEK(v.fecha, 3) AS semana, v.estado, COUNT(*) AS cantidad
FROM v_cita v
WHERE v.id_medico = 1
  AND v.fecha BETWEEN @desde AND @hasta
GROUP BY YEARWEEK(v.fecha, 3), v.estado
ORDER BY semana, v.estado;

-- -----------------------------------------------------------------------------
-- 4. Reporte 2 (RF09): pagos validados por período (cantidad y total en S/)
-- -----------------------------------------------------------------------------
SELECT DATE(pg.validado_en) AS dia,
       COUNT(*)             AS pagos_validados,
       SUM(pg.monto)        AS total_soles
FROM pago pg
         JOIN cita c ON c.id_cita = pg.id_cita
WHERE c.id_medico = 1
  AND pg.estado = 'VALIDADO'
  AND pg.validado_en >= @desde
  AND pg.validado_en < @hasta + INTERVAL 1 DAY
GROUP BY DATE(pg.validado_en) WITH ROLLUP;

-- -----------------------------------------------------------------------------
-- 5. Disponibilidad (RF05): bloques libres de un médico en una fecha
--    Libre = no bloqueado y sin cita activa (RN-01). Por defecto, el próximo día hábil.
-- -----------------------------------------------------------------------------
SET @fecha = (SELECT MIN(fecha) FROM horario WHERE id_medico = 1 AND fecha > CURDATE());

SELECT h.id_horario, h.fecha, h.hora_inicio, h.hora_fin
FROM horario h
WHERE h.id_medico = 1
  AND h.fecha = @fecha
  AND h.bloqueado = FALSE
  AND NOT EXISTS (SELECT 1 FROM cita_horario ch
                  WHERE ch.id_horario = h.id_horario AND ch.activo = TRUE)
ORDER BY h.hora_inicio;

-- Inicios válidos para una cita NUEVO (60 min = 2 bloques libres consecutivos)
SELECT h1.hora_inicio, h2.hora_fin
FROM horario h1
         JOIN horario h2 ON h2.id_medico = h1.id_medico AND h2.fecha = h1.fecha
                        AND h2.hora_inicio = h1.hora_fin
WHERE h1.id_medico = 1
  AND h1.fecha = @fecha
  AND h1.bloqueado = FALSE AND h2.bloqueado = FALSE
  AND NOT EXISTS (SELECT 1 FROM cita_horario ch
                  WHERE ch.id_horario IN (h1.id_horario, h2.id_horario) AND ch.activo = TRUE)
ORDER BY h1.hora_inicio;

-- -----------------------------------------------------------------------------
-- 6. Pruebas de las constraints: cada INSERT/UPDATE de esta sección DEBE FALLAR
-- -----------------------------------------------------------------------------

-- 6.1 RN-01: un bloque ya ocupado por una cita activa no admite otra.
--     Se toma un bloque de una cita PROGRAMADA y se intenta asignarlo a otra cita.
SET @bloque_ocupado = (SELECT ch.id_horario
                       FROM cita_horario ch JOIN cita c ON c.id_cita = ch.id_cita
                       WHERE c.estado = 'PROGRAMADA' AND ch.activo = TRUE
                       ORDER BY ch.id_horario LIMIT 1);
SET @otra_cita = (SELECT id_cita FROM cita WHERE estado = 'ATENDIDA' ORDER BY id_cita LIMIT 1);

-- Esperado: ERROR 1062 Duplicate entry ... for key 'cita_horario.uq_cita_horario_activo'
INSERT INTO cita_horario (id_cita, id_horario) VALUES (@otra_cita, @bloque_ocupado);

-- 6.2 RN-09: un paciente con una cita activa no puede tener una segunda.
SET @paciente_activo = (SELECT id_paciente FROM cita WHERE estado = 'PROGRAMADA' ORDER BY id_cita LIMIT 1);

-- Esperado: ERROR 1062 Duplicate entry ... for key 'cita.uq_cita_paciente_activo'
INSERT INTO cita (id_paciente, id_medico, tipo_cita, estado)
VALUES (@paciente_activo, 1, 'CONTROL', 'PENDIENTE_PAGO');

-- 6.3 RN-12: el n.º de documento es único.
-- Esperado: ERROR 1062 Duplicate entry '70000001' for key 'usuario.uq_usuario_documento'
INSERT INTO usuario (tipo_documento, numero_documento, contrasena_hash, rol)
VALUES ('DNI', '70000001', 'x', 'PACIENTE');

-- 6.4 RF02: un DNI debe tener 8 dígitos.
-- Esperado: ERROR 3819 Check constraint 'ck_usuario_documento' is violated.
INSERT INTO usuario (tipo_documento, numero_documento, contrasena_hash, rol)
VALUES ('DNI', '1234', 'x', 'PACIENTE');

-- 6.5 RN-13: no se cancela sin observación.
-- Esperado: ERROR 3819 Check constraint 'ck_cita_cancelacion' is violated.
UPDATE cita SET estado = 'CANCELADA' WHERE estado = 'PROGRAMADA' ORDER BY id_cita LIMIT 1;

-- 6.6 Un pago por cita y monto positivo.
-- Esperado: ERROR 3819 Check constraint 'ck_pago_monto' is violated.
INSERT INTO pago (id_cita, monto, comprobante_ruta, comprobante_tipo)
VALUES (@otra_cita, 0, 'uploads/comprobantes/x.png', 'image/png');

-- 6.7 Bloque duplicado para el mismo médico, fecha y hora.
-- Esperado: ERROR 1062 Duplicate entry ... for key 'horario.uq_horario_medico_inicio'
INSERT INTO horario (id_medico, fecha, hora_inicio, hora_fin)
SELECT id_medico, fecha, hora_inicio, hora_fin FROM horario ORDER BY id_horario LIMIT 1;

-- 6.8 Contraprueba (sí debe funcionar): al cancelar, el bloque y el paciente quedan libres.
START TRANSACTION;
SET @cita_cancelar = (SELECT id_cita FROM cita WHERE estado = 'PROGRAMADA' ORDER BY id_cita LIMIT 1);
UPDATE cita SET estado = 'CANCELADA', cancelada_por = 'MEDICO', observacion = 'Prueba de liberación'
WHERE id_cita = @cita_cancelar;
UPDATE cita_horario SET activo = FALSE WHERE id_cita = @cita_cancelar;
SELECT id_cita, estado, paciente_activo FROM cita WHERE id_cita = @cita_cancelar;   -- paciente_activo = NULL
SELECT id_horario, activo, horario_activo FROM cita_horario WHERE id_cita = @cita_cancelar; -- horario_activo = NULL
ROLLBACK;
