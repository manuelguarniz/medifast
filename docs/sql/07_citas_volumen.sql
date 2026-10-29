-- =============================================================================
-- MEDIFAST — 07_citas_volumen.sql
-- Carga de volumen: completa la tabla cita hasta 100 000 registros con el
-- médico actual (CMP 000001), para pruebas de rendimiento de reportes, agenda
-- y paginación. Ejecutar después de 02_datos_prueba.sql (y de
-- 06_citas_historicas.sql si se usa).
--
-- Uso (con root: medifast_app no tiene el privilegio CREATE ROUTINE):
--   mysql -u root -p < docs/sql/07_citas_volumen.sql
--   docker exec -i medifast-mysql mysql -uroot -proot < docs/sql/07_citas_volumen.sql
--
-- Cómo se arma:
--   - Crea 3 000 pacientes ficticios (DNI 72000001 … 72003000, contraseña
--     Medifast2026). Cada uno es paciente del consultorio durante un periodo
--     de 1 a 6 años y solo reserva dentro de ese periodo.
--   - Los bloques se generan antes del primer bloque existente, con un horario
--     histórico ampliado: lunes a sábado, 08:00–20:00 (24 bloques por día). Es
--     un horario ficticio para que quepa el volumen; horario_semanal no cambia.
--   - Se recorre en orden cronológico y se detiene al llegar al total pedido.
--
-- Reglas que respeta: RN-01 (una cita activa por bloque; las CANCELADAS
-- liberan su bloque), RN-02 (NUEVO 60 min hasta la primera ATENDIDA, luego
-- CONTROL 30 min), RN-06 (reserva entre 1 y 30 días antes), RN-13 (cancelación
-- con observación y responsable). Solo estados finales: RN-09 no se toca.
-- Los pacientes nuevos solo tienen citas en este periodo, así que su historial
-- no choca con el de los pacientes de 02/06.
--
-- No es re-ejecutable: se detiene si ya existe el paciente DNI 72000001.
-- =============================================================================

USE medifast;

DROP PROCEDURE IF EXISTS sp_seed_volumen;

DELIMITER //
CREATE PROCEDURE sp_seed_volumen(IN p_total INT, IN p_pacientes INT)
BEGIN
    DECLARE v_medico    BIGINT;
    DECLARE v_faltan    INT;
    DECLARE v_desde     DATE;
    DECLARE v_hasta     DATE;
    DECLARE v_ultimo    DATE;
    DECLARE v_fecha     DATE;
    DECLARE v_hora      TIME;
    DECLARE v_paciente  BIGINT;
    DECLARE v_atendido  BOOLEAN;
    DECLARE v_tipo      VARCHAR(10);
    DECLARE v_duracion  SMALLINT;
    DECLARE v_precio    DECIMAL(10, 2);
    DECLARE v_r         DOUBLE;
    DECLARE v_estado    VARCHAR(20);
    DECLARE v_pago      VARCHAR(20);
    DECLARE v_cancelada VARCHAR(10);
    DECLARE v_obs       VARCHAR(500);
    DECLARE v_creado    DATETIME;
    DECLARE v_cita      BIGINT;

    IF EXISTS(SELECT 1 FROM usuario WHERE numero_documento = '72000001') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La carga de volumen ya se ejecutó (existe el paciente DNI 72000001)';
    END IF;

    SELECT COUNT(*) INTO v_faltan FROM cita;
    SET v_faltan = p_total - v_faltan;
    IF v_faltan <= 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La tabla cita ya tiene el total pedido';
    END IF;

    SELECT id_medico INTO v_medico FROM medico WHERE cmp = '000001';
    SELECT MIN(fecha) - INTERVAL 1 DAY INTO v_hasta FROM horario WHERE id_medico = v_medico;
    SET v_hasta = IFNULL(v_hasta, CURDATE() - INTERVAL 31 DAY);
    -- ~23 citas por día hábil, ~0.83 días hábiles por día calendario; el
    -- recorrido se detiene al llegar al total, así que se estima de más.
    SET v_desde = v_hasta - INTERVAL CEIL(v_faltan / 23 / 0.83) DAY;

    -- Bloques de 30 min: lunes a sábado, 08:00–20:00
    INSERT INTO horario (id_medico, fecha, hora_inicio, hora_fin, creado_en)
    WITH RECURSIVE
        dias AS (SELECT v_desde AS fecha
                 UNION ALL
                 SELECT fecha + INTERVAL 1 DAY FROM dias WHERE fecha < v_hasta),
        bloques AS (SELECT CAST('08:00:00' AS TIME) AS inicio
                    UNION ALL
                    SELECT ADDTIME(inicio, '00:30:00') FROM bloques WHERE inicio < CAST('19:30:00' AS TIME))
    SELECT v_medico, d.fecha, b.inicio, ADDTIME(b.inicio, '00:30:00'),
           TIMESTAMP(d.fecha - INTERVAL 30 DAY, '08:00:00')
    FROM dias d
             CROSS JOIN bloques b
    WHERE WEEKDAY(d.fecha) < 6
    ORDER BY d.fecha, b.inicio;

    -- Pacientes ficticios con su periodo de atención
    DROP TEMPORARY TABLE IF EXISTS tmp_paciente;
    CREATE TEMPORARY TABLE tmp_paciente (
        n           INT     PRIMARY KEY,
        id_paciente BIGINT  NULL,
        inicio      DATE    NOT NULL,
        fin         DATE    NOT NULL,
        atendido    BOOLEAN NOT NULL DEFAULT FALSE,
        ultimo_dia  DATE    NULL,
        INDEX idx_tmp_periodo (inicio, fin)
    );

    INSERT INTO tmp_paciente (n, inicio, fin)
    WITH RECURSIVE seq AS (SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < p_pacientes)
    SELECT n, inicio, inicio + INTERVAL (365 + FLOOR(RAND() * 1826)) DAY
    FROM (SELECT n, v_desde + INTERVAL FLOOR(RAND() * (DATEDIFF(v_hasta, v_desde) + 730)) DAY
                    - INTERVAL 730 DAY AS inicio
          FROM seq) s;

    INSERT INTO usuario (tipo_documento, numero_documento, contrasena_hash, rol, creado_en)
    SELECT 'DNI', CONCAT('72', LPAD(n, 6, '0')),
           '$2a$10$Kp2Urj3xvFRlV6JkzAALN.03p/dIZstkzKJAbQ3ovmtqLPTkOMzDm', 'PACIENTE',
           TIMESTAMP(inicio - INTERVAL 31 DAY, '10:00:00')
    FROM tmp_paciente;

    INSERT INTO paciente (id_usuario, nombres, apellidos, fecha_nacimiento, celular, correo, creado_en)
    SELECT u.id_usuario,
           ELT(1 + MOD(t.n, 20), 'Lucía', 'Carlos', 'María', 'Jorge', 'Rosa', 'Luis', 'Ana', 'Pedro',
               'Carmen', 'Miguel', 'Patricia', 'José', 'Elena', 'Raúl', 'Sofía', 'Andrés',
               'Gabriela', 'Fernando', 'Valeria', 'Ricardo'),
           CONCAT(ELT(1 + MOD(t.n * 7, 25), 'Ramírez', 'Mendoza', 'Quispe', 'Castillo', 'Paredes',
                      'Gutiérrez', 'Flores', 'Vásquez', 'Díaz', 'Sánchez', 'Reyes', 'Herrera', 'Cruz',
                      'Romero', 'Alvarado', 'Navarro', 'Ponce', 'Cáceres', 'Espinoza', 'Benites',
                      'Zavaleta', 'Luján', 'Rodríguez', 'Valverde', 'Mostacero'),
                  ' ',
                  ELT(1 + MOD(t.n * 13, 25), 'Torres', 'Ríos', 'Huamán', 'Vega', 'León', 'Salazar',
                      'Chávez', 'Rojas', 'Morales', 'Ortiz', 'Campos', 'Núñez', 'Medina', 'Aguilar',
                      'Pinto', 'Silva', 'Ibáñez', 'Lozano', 'Ruiz', 'Cárdenas', 'Arana', 'Polo',
                      'Plasencia', 'Otiniano', 'Ávila')),
           t.inicio - INTERVAL (18 + FLOOR(RAND() * 60)) YEAR - INTERVAL FLOOR(RAND() * 365) DAY,
           CONCAT('97', LPAD(t.n, 7, '0')),
           CONCAT('paciente', LPAD(t.n, 6, '0'), '@correo.test'),
           u.creado_en
    FROM tmp_paciente t
             JOIN usuario u ON u.numero_documento = CONCAT('72', LPAD(t.n, 6, '0'));

    UPDATE tmp_paciente t
        JOIN usuario u ON u.numero_documento = CONCAT('72', LPAD(t.n, 6, '0'))
        JOIN paciente p ON p.id_usuario = u.id_usuario
    SET t.id_paciente = p.id_paciente;

    -- Recorre cada día con bloques y cada bloque en orden cronológico
    SET v_fecha = v_desde;
    WHILE v_fecha <= v_hasta AND v_faltan > 0 DO
        SELECT MIN(hora_inicio) INTO v_hora
        FROM horario WHERE id_medico = v_medico AND fecha = v_fecha;

        -- ~3 % de días sin atención (vacaciones, feriados)
        IF v_hora IS NOT NULL AND RAND() < 0.03 THEN
            SET v_hora = NULL;
        END IF;

        WHILE v_hora IS NOT NULL AND v_faltan > 0 DO
            -- ~12 % de bloques quedan libres
            IF RAND() < 0.12 THEN
                SELECT MIN(hora_inicio) INTO v_hora FROM horario
                WHERE id_medico = v_medico AND fecha = v_fecha AND hora_inicio > v_hora;
            ELSE
                -- Paciente al azar, en su periodo de atención y sin otra cita ese día
                SET v_paciente = NULL;
                SELECT id_paciente, atendido INTO v_paciente, v_atendido
                FROM tmp_paciente
                WHERE inicio <= v_fecha AND fin >= v_fecha
                  AND (ultimo_dia IS NULL OR ultimo_dia <> v_fecha)
                ORDER BY RAND() LIMIT 1;

                SET v_tipo = IF(v_atendido, 'CONTROL', 'NUEVO');
                SELECT duracion_min, precio INTO v_duracion, v_precio
                FROM tarifa WHERE id_medico = v_medico AND tipo_cita = v_tipo;

                -- Una cita NUEVO necesita bloques libres que cubran sus 60 min
                IF v_paciente IS NULL
                    OR (SELECT COUNT(*) FROM horario
                        WHERE id_medico = v_medico AND fecha = v_fecha
                          AND hora_inicio >= v_hora
                          AND hora_inicio < ADDTIME(v_hora, SEC_TO_TIME(v_duracion * 60)))
                       < v_duracion / 30 THEN
                    SELECT MIN(hora_inicio) INTO v_hora FROM horario
                    WHERE id_medico = v_medico AND fecha = v_fecha AND hora_inicio > v_hora;
                ELSE
                    SET v_r = RAND();
                    SET v_estado = 'ATENDIDA', v_pago = 'VALIDADO', v_cancelada = NULL, v_obs = NULL;
                    IF v_r < 0.08 THEN
                        SET v_estado = 'NO_ASISTIO';
                    ELSEIF v_r < 0.16 THEN
                        SET v_estado = 'CANCELADA', v_pago = NULL, v_cancelada = 'SISTEMA',
                            v_obs = 'Tiempo de reserva vencido';
                    ELSEIF v_r < 0.19 THEN
                        SET v_estado = 'CANCELADA', v_pago = 'RECHAZADO', v_cancelada = 'MEDICO',
                            v_obs = ELT(1 + FLOOR(RAND() * 3),
                                        'Pago rechazado: constancia ilegible',
                                        'Pago rechazado: el monto no coincide con la tarifa',
                                        'Pago rechazado: número de operación no encontrado');
                    ELSEIF v_r < 0.21 THEN
                        SET v_estado = 'CANCELADA', v_pago = 'POR_VALIDAR', v_cancelada = 'MEDICO',
                            v_obs = ELT(1 + FLOOR(RAND() * 3),
                                        'El médico no atenderá ese día por congreso',
                                        'El médico no atenderá por una emergencia',
                                        'Reserva duplicada por error del paciente');
                    END IF;

                    -- RN-06: reservada entre 1 y 30 días antes (sesgado a pocos días)
                    SET v_creado = TIMESTAMP(v_fecha - INTERVAL (1 + FLOOR(POW(RAND(), 2) * 30)) DAY,
                                             SEC_TO_TIME(FLOOR(7 * 3600 + RAND() * 15 * 3600)));

                    INSERT INTO cita (id_paciente, id_medico, tipo_cita, estado, observacion,
                                      cancelada_por, fecha, hora_inicio, hora_fin, creado_en, actualizado_en)
                    VALUES (v_paciente, v_medico, v_tipo, v_estado, v_obs, v_cancelada,
                            v_fecha, v_hora, ADDTIME(v_hora, SEC_TO_TIME(v_duracion * 60)), v_creado,
                            IF(v_estado = 'CANCELADA', v_creado + INTERVAL 15 MINUTE,
                               TIMESTAMP(v_fecha, ADDTIME(v_hora, SEC_TO_TIME(v_duracion * 60)))));
                    SET v_cita = LAST_INSERT_ID();
                    SET v_faltan = v_faltan - 1;
                    SET v_ultimo = v_fecha;

                    -- CANCELADA => bloque liberado (activo = FALSE)
                    INSERT INTO cita_horario (id_cita, id_horario, activo, creado_en)
                    SELECT v_cita, id_horario, v_estado <> 'CANCELADA', v_creado
                    FROM horario
                    WHERE id_medico = v_medico AND fecha = v_fecha
                      AND hora_inicio >= v_hora
                      AND hora_inicio < ADDTIME(v_hora, SEC_TO_TIME(v_duracion * 60));

                    IF v_pago IS NOT NULL THEN
                        INSERT INTO pago (id_cita, monto, nro_operacion, comprobante_ruta, comprobante_tipo,
                                          estado, fecha_pago, validado_en, creado_en)
                        VALUES (v_cita, v_precio,
                                IF(MOD(v_cita, 3) = 0, NULL, LPAD(v_cita * 7919 MOD 100000000, 8, '0')),
                                CONCAT('uploads/comprobantes/', UUID(), IF(MOD(v_cita, 4) = 0, '.pdf', '.png')),
                                IF(MOD(v_cita, 4) = 0, 'application/pdf', 'image/png'),
                                v_pago,
                                v_creado + INTERVAL 10 MINUTE,
                                IF(v_pago = 'POR_VALIDAR', NULL,
                                   v_creado + INTERVAL (30 + FLOOR(RAND() * 300)) MINUTE),
                                v_creado + INTERVAL 10 MINUTE);
                    END IF;

                    IF v_estado = 'ATENDIDA' THEN
                        UPDATE tmp_paciente SET atendido = TRUE WHERE id_paciente = v_paciente;
                    END IF;

                    -- Si se canceló, el mismo bloque se vuelve a ofrecer
                    IF v_estado <> 'CANCELADA' THEN
                        UPDATE tmp_paciente SET ultimo_dia = v_fecha WHERE id_paciente = v_paciente;
                        SELECT MIN(hora_inicio) INTO v_hora FROM horario
                        WHERE id_medico = v_medico AND fecha = v_fecha
                          AND hora_inicio >= ADDTIME(v_hora, SEC_TO_TIME(v_duracion * 60));
                    END IF;
                END IF;
            END IF;
        END WHILE;

        SET v_fecha = v_fecha + INTERVAL 1 DAY;
    END WHILE;

    -- Si se llegó al total antes de v_hasta, quita los días sobrantes (sin citas)
    DELETE h FROM horario h
    WHERE h.id_medico = v_medico AND h.fecha > v_ultimo AND h.fecha <= v_hasta
      AND NOT EXISTS (SELECT 1 FROM cita_horario ch WHERE ch.id_horario = h.id_horario);

    DROP TEMPORARY TABLE tmp_paciente;
END //
DELIMITER ;

SET SESSION cte_max_recursion_depth = 100000;
START TRANSACTION;
CALL sp_seed_volumen(100000, 3000);
COMMIT;

DROP PROCEDURE sp_seed_volumen;

-- Resumen
SELECT estado, COUNT(*) AS citas FROM cita GROUP BY estado ORDER BY estado;
SELECT tipo_cita, COUNT(*) AS citas FROM cita GROUP BY tipo_cita;
SELECT COUNT(*) AS total_citas, MIN(fecha) AS desde, MAX(fecha) AS hasta FROM v_cita;
SELECT COUNT(*) AS pacientes FROM paciente;
