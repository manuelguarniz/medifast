-- =============================================================================
-- MEDIFAST — 06_citas_historicas.sql
-- Carga masiva de citas históricas (~5 años, más de 10 000 citas) para todos
-- los pacientes registrados. Sirve para probar reportes, paginación y "Mis
-- citas" con volumen. Ejecutar después de 02_datos_prueba.sql.
--
-- Uso (con root: medifast_app no tiene el privilegio CREATE ROUTINE):
--   mysql -u root -p < docs/sql/06_citas_historicas.sql
--   docker exec -i medifast-mysql mysql -uroot -proot < docs/sql/06_citas_historicas.sql
--
-- Reglas que respeta:
--   - Los bloques se generan desde horario_semanal, antes del primer bloque
--     existente, así que no se cruzan con los datos de 02_datos_prueba.sql.
--   - RN-01: una cita activa por bloque; las CANCELADAS liberan sus bloques y
--     el bloque se vuelve a reservar.
--   - RN-02: NUEVO (60 min) si el paciente aún no tiene una cita ATENDIDA con
--     el médico; CONTROL (30 min) en otro caso. Por eso se recorre en orden
--     cronológico.
--   - RN-06: la reserva se crea entre 1 y 30 días antes de la cita.
--   - RN-13: toda cancelación lleva observación y responsable.
--   - Solo estados finales (ATENDIDA, NO_ASISTIO, CANCELADA): RN-09 no se toca.
--
-- No es re-ejecutable: se detiene si ya hay bloques de más de 60 días atrás.
-- Para recargar:  docker compose down -v && docker compose up -d  y volver a
-- ejecutar este script.
-- =============================================================================

USE medifast;

DROP PROCEDURE IF EXISTS sp_seed_historico;

DELIMITER //
CREATE PROCEDURE sp_seed_historico(IN p_anios INT)
BEGIN
    DECLARE v_medico    BIGINT;
    DECLARE v_desde     DATE;
    DECLARE v_hasta     DATE;
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

    IF EXISTS(SELECT 1 FROM horario WHERE fecha < CURDATE() - INTERVAL 60 DAY) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Las citas históricas ya se cargaron (hay bloques de más de 60 días atrás)';
    END IF;

    SELECT id_medico INTO v_medico FROM medico WHERE cmp = '000001';
    SELECT MIN(fecha) - INTERVAL 1 DAY INTO v_hasta FROM horario WHERE id_medico = v_medico;
    SET v_hasta = IFNULL(v_hasta, CURDATE() - INTERVAL 31 DAY);
    SET v_desde = v_hasta - INTERVAL p_anios YEAR;

    -- Bloques de 30 min del periodo histórico, según la plantilla semanal
    INSERT INTO horario (id_medico, fecha, hora_inicio, hora_fin, creado_en)
    WITH RECURSIVE
        dias AS (SELECT v_desde AS fecha
                 UNION ALL
                 SELECT fecha + INTERVAL 1 DAY FROM dias WHERE fecha < v_hasta),
        bloques AS (SELECT CAST('00:00:00' AS TIME) AS inicio
                    UNION ALL
                    SELECT ADDTIME(inicio, '00:30:00') FROM bloques WHERE inicio < CAST('23:00:00' AS TIME))
    SELECT hs.id_medico, d.fecha, b.inicio, ADDTIME(b.inicio, '00:30:00'),
           TIMESTAMP(d.fecha - INTERVAL 30 DAY, '08:00:00')
    FROM dias d
             JOIN horario_semanal hs ON hs.id_medico = v_medico AND hs.dia_semana = WEEKDAY(d.fecha) + 1
             JOIN bloques b ON b.inicio >= hs.hora_inicio AND ADDTIME(b.inicio, '00:30:00') <= hs.hora_fin
    ORDER BY d.fecha, b.inicio;

    -- Estado por paciente: si ya fue atendido (RN-02) y el último día con cita
    DROP TEMPORARY TABLE IF EXISTS tmp_paciente;
    CREATE TEMPORARY TABLE tmp_paciente (
        id_paciente BIGINT PRIMARY KEY,
        atendido    BOOLEAN NOT NULL DEFAULT FALSE,
        ultimo_dia  DATE    NULL
    );
    INSERT INTO tmp_paciente (id_paciente) SELECT id_paciente FROM paciente;

    -- Recorre cada día con bloques y cada bloque en orden cronológico
    SET v_fecha = v_desde;
    WHILE v_fecha <= v_hasta DO
        SELECT MIN(hora_inicio) INTO v_hora
        FROM horario WHERE id_medico = v_medico AND fecha = v_fecha;

        -- ~3 % de días sin atención (vacaciones, feriados)
        IF v_hora IS NOT NULL AND RAND() < 0.03 THEN
            SET v_hora = NULL;
        END IF;

        WHILE v_hora IS NOT NULL DO
            -- ~12 % de bloques quedan libres
            IF RAND() < 0.12 THEN
                SELECT MIN(hora_inicio) INTO v_hora FROM horario
                WHERE id_medico = v_medico AND fecha = v_fecha AND hora_inicio > v_hora;
            ELSE
                -- Paciente al azar que no tenga otra cita ese día
                SET v_paciente = NULL;
                SELECT id_paciente, atendido INTO v_paciente, v_atendido
                FROM tmp_paciente
                WHERE ultimo_dia IS NULL OR ultimo_dia <> v_fecha
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

    DROP TEMPORARY TABLE tmp_paciente;

    -- El paciente se registró antes de su primera reserva
    UPDATE usuario u
        JOIN paciente p ON p.id_usuario = u.id_usuario
        JOIN (SELECT id_paciente, MIN(creado_en) AS primera FROM cita GROUP BY id_paciente) c
             ON c.id_paciente = p.id_paciente
    SET u.creado_en = LEAST(u.creado_en, c.primera - INTERVAL 1 DAY),
        p.creado_en = LEAST(p.creado_en, c.primera - INTERVAL 1 DAY);
END //
DELIMITER ;

SET SESSION cte_max_recursion_depth = 10000;
START TRANSACTION;
CALL sp_seed_historico(5);
COMMIT;

DROP PROCEDURE sp_seed_historico;

-- Resumen
SELECT estado, COUNT(*) AS citas FROM cita GROUP BY estado ORDER BY estado;
SELECT tipo_cita, COUNT(*) AS citas FROM cita GROUP BY tipo_cita;
SELECT COUNT(*) AS total_citas, MIN(fecha) AS desde, MAX(fecha) AS hasta FROM v_cita;
