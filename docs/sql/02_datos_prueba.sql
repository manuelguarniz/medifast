-- =============================================================================
-- MEDIFAST — 02_datos_prueba.sql
-- Datos de prueba (README §13.1). Ejecutar después de
-- 01_schema.sql. Las fechas son relativas al día de ejecución (CURDATE()).
--
-- Credenciales de prueba (todas las cuentas):  contraseña  Medifast2026
--   Médico:    DNI 10000001
--   Pacientes: DNI 70000001 … 70000025   ·   CE 001000026 … 001000030
--
-- Todos los datos personales son ficticios, salvo el nombre del médico y del
-- consultorio. El DNI y el CMP del médico son ficticios.
-- =============================================================================

USE medifast;

-- Hash bcrypt (coste 10) de "Medifast2026"
SET @hash_prueba = '$2a$10$Kp2Urj3xvFRlV6JkzAALN.03p/dIZstkzKJAbQ3ovmtqLPTkOMzDm';

-- -----------------------------------------------------------------------------
-- Consultorio, médico y tarifas (RN-03, RN-06)
-- -----------------------------------------------------------------------------
INSERT INTO consultorio (nombre, direccion, dias_anticipacion_min, dias_anticipacion_max,
                         minutos_bloque, minutos_apartado)
VALUES ('Medifast', 'Trujillo, La Libertad', 1, 30, 30, 15);
SET @id_consultorio = LAST_INSERT_ID();

INSERT INTO usuario (tipo_documento, numero_documento, contrasena_hash, rol)
VALUES ('DNI', '10000001', @hash_prueba, 'MEDICO');

INSERT INTO medico (id_usuario, id_consultorio, nombres, apellidos, cmp, especialidad)
VALUES (LAST_INSERT_ID(), @id_consultorio, 'Moisés', 'Barrantes Cabrera', '000001',
        'Medicina Interna y Reumatología');
SET @id_medico = LAST_INSERT_ID();

INSERT INTO tarifa (id_medico, tipo_cita, duracion_min, precio)
VALUES (@id_medico, 'NUEVO', 60, 150.00),
       (@id_medico, 'CONTROL', 30, 80.00);

-- -----------------------------------------------------------------------------
-- Horario semanal: lunes (1) a sábado (6), 09:00–13:00 (RN-08)
-- -----------------------------------------------------------------------------
INSERT INTO horario_semanal (id_medico, dia_semana, hora_inicio, hora_fin)
VALUES (@id_medico, 1, '09:00', '13:00'),
       (@id_medico, 2, '09:00', '13:00'),
       (@id_medico, 3, '09:00', '13:00'),
       (@id_medico, 4, '09:00', '13:00'),
       (@id_medico, 5, '09:00', '13:00'),
       (@id_medico, 6, '09:00', '13:00');

-- -----------------------------------------------------------------------------
-- Bloques de 30 min desde 30 días atrás hasta 30 días adelante
-- (en producción los genera la aplicación a partir de horario_semanal)
-- -----------------------------------------------------------------------------
INSERT INTO horario (id_medico, fecha, hora_inicio, hora_fin)
WITH RECURSIVE
    dias AS (SELECT CURDATE() - INTERVAL 30 DAY AS fecha
             UNION ALL
             SELECT fecha + INTERVAL 1 DAY FROM dias WHERE fecha < CURDATE() + INTERVAL 30 DAY),
    bloques AS (SELECT CAST('09:00:00' AS TIME) AS inicio
                UNION ALL
                SELECT ADDTIME(inicio, '00:30:00') FROM bloques WHERE inicio < CAST('12:30:00' AS TIME))
SELECT hs.id_medico, d.fecha, b.inicio, ADDTIME(b.inicio, '00:30:00')
FROM dias d
         JOIN horario_semanal hs ON hs.id_medico = @id_medico AND hs.dia_semana = WEEKDAY(d.fecha) + 1
         JOIN bloques b ON b.inicio >= hs.hora_inicio AND ADDTIME(b.inicio, '00:30:00') <= hs.hora_fin
ORDER BY d.fecha, b.inicio;

-- -----------------------------------------------------------------------------
-- Pacientes ficticios: 25 con DNI y 5 con CE (RN-12)
-- -----------------------------------------------------------------------------
CREATE TEMPORARY TABLE seed_paciente (
    tipo_documento   VARCHAR(3),
    numero_documento VARCHAR(12),
    nombres          VARCHAR(100),
    apellidos        VARCHAR(100),
    fecha_nacimiento DATE,
    celular          CHAR(9),
    correo           VARCHAR(150)
);

INSERT INTO seed_paciente VALUES
    ('DNI', '70000001', 'Lucía',      'Ramírez Torres',    '1985-03-12', '987000001', 'lucia.ramirez@correo.test'),
    ('DNI', '70000002', 'Carlos',     'Mendoza Ríos',      '1979-07-25', '987000002', 'carlos.mendoza@correo.test'),
    ('DNI', '70000003', 'María',      'Quispe Huamán',     '1992-11-03', '987000003', 'maria.quispe@correo.test'),
    ('DNI', '70000004', 'Jorge',      'Castillo Vega',     '1968-01-19', '987000004', 'jorge.castillo@correo.test'),
    ('DNI', '70000005', 'Rosa',       'Paredes León',      '1975-05-30', '987000005', 'rosa.paredes@correo.test'),
    ('DNI', '70000006', 'Luis',       'Gutiérrez Salazar', '1988-09-14', '987000006', 'luis.gutierrez@correo.test'),
    ('DNI', '70000007', 'Ana',        'Flores Chávez',     '1995-12-08', '987000007', 'ana.flores@correo.test'),
    ('DNI', '70000008', 'Pedro',      'Vásquez Rojas',     '1960-04-22', '987000008', 'pedro.vasquez@correo.test'),
    ('DNI', '70000009', 'Carmen',     'Díaz Morales',      '1983-06-17', '987000009', 'carmen.diaz@correo.test'),
    ('DNI', '70000010', 'Miguel',     'Sánchez Ortiz',     '1990-02-28', '987000010', 'miguel.sanchez@correo.test'),
    ('DNI', '70000011', 'Patricia',   'Reyes Campos',      '1972-10-05', '987000011', 'patricia.reyes@correo.test'),
    ('DNI', '70000012', 'José',       'Herrera Núñez',     '1986-08-11', '987000012', 'jose.herrera@correo.test'),
    ('DNI', '70000013', 'Elena',      'Cruz Medina',       '1999-03-27', '987000013', 'elena.cruz@correo.test'),
    ('DNI', '70000014', 'Raúl',       'Romero Aguilar',    '1965-12-30', '987000014', 'raul.romero@correo.test'),
    ('DNI', '70000015', 'Sofía',      'Alvarado Pinto',    '2001-07-09', '987000015', 'sofia.alvarado@correo.test'),
    ('DNI', '70000016', 'Andrés',     'Navarro Silva',     '1981-05-16', '987000016', 'andres.navarro@correo.test'),
    ('DNI', '70000017', 'Gabriela',   'Ponce Ibáñez',      '1993-01-23', '987000017', 'gabriela.ponce@correo.test'),
    ('DNI', '70000018', 'Fernando',   'Cáceres Lozano',    '1977-11-02', '987000018', 'fernando.caceres@correo.test'),
    ('DNI', '70000019', 'Valeria',    'Espinoza Ruiz',     '1997-09-19', '987000019', 'valeria.espinoza@correo.test'),
    ('DNI', '70000020', 'Ricardo',    'Benites Cárdenas',  '1970-06-06', '987000020', 'ricardo.benites@correo.test'),
    ('DNI', '70000021', 'Isabel',     'Zavaleta Arana',    '1984-04-14', '987000021', 'isabel.zavaleta@correo.test'),
    ('DNI', '70000022', 'Óscar',      'Luján Polo',        '1962-02-10', '987000022', 'oscar.lujan@correo.test'),
    ('DNI', '70000023', 'Daniela',    'Rodríguez Plasencia','1994-08-29','987000023', 'daniela.rodriguez@correo.test'),
    ('DNI', '70000024', 'Héctor',     'Valverde Otiniano', '1958-10-21', '987000024', 'hector.valverde@correo.test'),
    ('DNI', '70000025', 'Claudia',    'Mostacero Ávila',   '1989-12-15', '987000025', 'claudia.mostacero@correo.test'),
    ('CE',  '001000026', 'Gabriel',   'Pérez Hernández',   '1987-03-04', '987000026', 'gabriel.perez@correo.test'),
    ('CE',  '001000027', 'Mariana',   'González Rivas',    '1991-07-18', '987000027', 'mariana.gonzalez@correo.test'),
    ('CE',  '001000028', 'Alejandro', 'Martínez Lugo',     '1974-11-26', '987000028', 'alejandro.martinez@correo.test'),
    ('CE',  '001000029', 'Daniel',    'Contreras Mora',    '1998-05-01', '987000029', 'daniel.contreras@correo.test'),
    ('CE',  '001000030', 'Andreína',  'Salas Bermúdez',    '1983-09-09', '987000030', 'andreina.salas@correo.test');

INSERT INTO usuario (tipo_documento, numero_documento, contrasena_hash, rol)
SELECT tipo_documento, numero_documento, @hash_prueba, 'PACIENTE'
FROM seed_paciente;

INSERT INTO paciente (id_usuario, nombres, apellidos, fecha_nacimiento, celular, correo)
SELECT u.id_usuario, s.nombres, s.apellidos, s.fecha_nacimiento, s.celular, s.correo
FROM seed_paciente s
         JOIN usuario u ON u.numero_documento = s.numero_documento;

DROP TEMPORARY TABLE seed_paciente;

-- -----------------------------------------------------------------------------
-- Procedimiento auxiliar (solo para el seed; se elimina al final).
-- Crea una cita con sus bloques y su pago:
--   p_dia_habil: n.º de día con horario contando desde hoy (-1 = último día
--                hábil pasado, 1 = próximo día hábil)
--   El tipo (NUEVO / CONTROL) se calcula según RN-02, por eso las llamadas van
--   en orden cronológico.
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_seed_cita;

DELIMITER //
CREATE PROCEDURE sp_seed_cita(
    IN p_documento     VARCHAR(12),
    IN p_dia_habil     INT,
    IN p_hora          TIME,
    IN p_estado        VARCHAR(20),
    IN p_estado_pago   VARCHAR(20),
    IN p_cancelada_por VARCHAR(10),
    IN p_observacion   VARCHAR(500)
)
BEGIN
    DECLARE v_medico   BIGINT;
    DECLARE v_paciente BIGINT;
    DECLARE v_offset   INT DEFAULT ABS(p_dia_habil) - 1;
    DECLARE v_fecha    DATE;
    DECLARE v_tipo     VARCHAR(10);
    DECLARE v_duracion SMALLINT;
    DECLARE v_precio   DECIMAL(10, 2);
    DECLARE v_creado   DATETIME;
    DECLARE v_cita     BIGINT;

    SELECT id_medico INTO v_medico FROM medico WHERE cmp = '000001';

    SELECT p.id_paciente INTO v_paciente
    FROM paciente p JOIN usuario u ON u.id_usuario = p.id_usuario
    WHERE u.numero_documento = p_documento;

    IF p_dia_habil > 0 THEN
        SELECT DISTINCT fecha INTO v_fecha FROM horario
        WHERE id_medico = v_medico AND fecha > CURDATE()
        ORDER BY fecha LIMIT 1 OFFSET v_offset;
    ELSE
        SELECT DISTINCT fecha INTO v_fecha FROM horario
        WHERE id_medico = v_medico AND fecha < CURDATE()
        ORDER BY fecha DESC LIMIT 1 OFFSET v_offset;
    END IF;

    -- RN-02: CONTROL si ya tiene una cita ATENDIDA con el médico
    SET v_tipo = IF(EXISTS(SELECT 1 FROM cita
                           WHERE id_paciente = v_paciente AND id_medico = v_medico
                             AND estado = 'ATENDIDA'), 'CONTROL', 'NUEVO');

    SELECT duracion_min, precio INTO v_duracion, v_precio
    FROM tarifa WHERE id_medico = v_medico AND tipo_cita = v_tipo;

    SET v_creado = IF(p_estado = 'PENDIENTE_PAGO', NOW(),
                      LEAST(TIMESTAMP(v_fecha - INTERVAL 2 DAY, '18:00:00'), NOW() - INTERVAL 1 HOUR));

    INSERT INTO cita (id_paciente, id_medico, tipo_cita, estado, expira_en, observacion,
                      cancelada_por, creado_en)
    VALUES (v_paciente, v_medico, v_tipo, p_estado,
            IF(p_estado = 'PENDIENTE_PAGO', NOW() + INTERVAL 15 MINUTE, NULL),
            p_observacion, p_cancelada_por, v_creado);
    SET v_cita = LAST_INSERT_ID();

    -- Bloques que cubren la duración de la tarifa (CANCELADA => bloque liberado)
    INSERT INTO cita_horario (id_cita, id_horario, activo)
    SELECT v_cita, id_horario, p_estado <> 'CANCELADA'
    FROM horario
    WHERE id_medico = v_medico AND fecha = v_fecha
      AND hora_inicio >= p_hora
      AND hora_inicio < ADDTIME(p_hora, SEC_TO_TIME(v_duracion * 60));

    IF p_estado_pago IS NOT NULL THEN
        INSERT INTO pago (id_cita, monto, nro_operacion, comprobante_ruta, comprobante_tipo,
                          estado, fecha_pago, validado_en)
        VALUES (v_cita, v_precio,
                IF(MOD(v_cita, 3) = 0, NULL, LPAD(v_cita * 7919, 8, '0')),
                CONCAT('uploads/comprobantes/', UUID(), IF(MOD(v_cita, 4) = 0, '.pdf', '.png')),
                IF(MOD(v_cita, 4) = 0, 'application/pdf', 'image/png'),
                p_estado_pago,
                v_creado + INTERVAL 10 MINUTE,
                IF(p_estado_pago = 'POR_VALIDAR', NULL,
                   LEAST(v_creado + INTERVAL 3 HOUR, NOW() - INTERVAL 30 MINUTE)));
    END IF;
END //
DELIMITER ;

-- -----------------------------------------------------------------------------
-- Citas en todos los estados (RN-05), en orden cronológico.
-- Cada cita empieza en una hora en punto, así NUEVO (60 min) nunca se cruza.
-- -----------------------------------------------------------------------------
-- Atendidas (primera consulta, NUEVO)
CALL sp_seed_cita('70000001', -20, '09:00', 'ATENDIDA', 'VALIDADO', NULL, NULL);
CALL sp_seed_cita('70000002', -20, '10:00', 'ATENDIDA', 'VALIDADO', NULL, NULL);
CALL sp_seed_cita('70000003', -19, '09:00', 'ATENDIDA', 'VALIDADO', NULL, NULL);
CALL sp_seed_cita('70000004', -19, '11:00', 'ATENDIDA', 'VALIDADO', NULL, NULL);
CALL sp_seed_cita('70000005', -18, '09:00', 'ATENDIDA', 'VALIDADO', NULL, NULL);
CALL sp_seed_cita('70000006', -18, '10:00', 'ATENDIDA', 'VALIDADO', NULL, NULL);
CALL sp_seed_cita('70000007', -17, '09:00', 'ATENDIDA', 'VALIDADO', NULL, NULL);
CALL sp_seed_cita('70000008', -17, '11:00', 'ATENDIDA', 'VALIDADO', NULL, NULL);
CALL sp_seed_cita('70000009', -16, '09:00', 'ATENDIDA', 'VALIDADO', NULL, NULL);
CALL sp_seed_cita('70000010', -16, '10:00', 'ATENDIDA', 'VALIDADO', NULL, NULL);

-- No asistió
CALL sp_seed_cita('70000011', -15, '09:00', 'NO_ASISTIO', 'VALIDADO', NULL, NULL);
CALL sp_seed_cita('70000012', -14, '10:00', 'NO_ASISTIO', 'VALIDADO', NULL, NULL);

-- Canceladas por el sistema: apartado de 15 min vencido, sin pago (RN-04, RF16)
CALL sp_seed_cita('70000013', -13, '09:00', 'CANCELADA', NULL, 'SISTEMA', 'Tiempo de reserva vencido');
CALL sp_seed_cita('70000014', -12, '10:00', 'CANCELADA', NULL, 'SISTEMA', 'Tiempo de reserva vencido');
CALL sp_seed_cita('70000015', -11, '11:00', 'CANCELADA', NULL, 'SISTEMA', 'Tiempo de reserva vencido');

-- Atendidas de control (CONTROL, 30 min)
CALL sp_seed_cita('70000001', -10, '09:00', 'ATENDIDA', 'VALIDADO', NULL, NULL);
CALL sp_seed_cita('70000002', -10, '10:00', 'ATENDIDA', 'VALIDADO', NULL, NULL);
CALL sp_seed_cita('70000003',  -9, '09:00', 'ATENDIDA', 'VALIDADO', NULL, NULL);
CALL sp_seed_cita('70000004',  -8, '11:00', 'NO_ASISTIO', 'VALIDADO', NULL, NULL);

-- Cancelada por el médico con el pago aún por validar (RF14, RN-13)
CALL sp_seed_cita('70000016',  -7, '09:00', 'CANCELADA', 'POR_VALIDAR', 'MEDICO', 'El médico no atenderá ese día por congreso');

-- Cancelada por pago rechazado (RF13, RN-07)
CALL sp_seed_cita('70000018',  -6, '09:00', 'CANCELADA', 'RECHAZADO', 'MEDICO', 'Pago rechazado: constancia ilegible');

-- Próximas citas
CALL sp_seed_cita('001000027',  1, '11:00', 'PENDIENTE_PAGO', NULL, NULL, NULL);
CALL sp_seed_cita('70000001',   1, '09:00', 'PROGRAMADA', 'VALIDADO', NULL, NULL);
CALL sp_seed_cita('70000005',   2, '09:00', 'PROGRAMADA', 'VALIDADO', NULL, NULL);
CALL sp_seed_cita('70000019',   2, '11:00', 'CANCELADA', 'RECHAZADO', 'MEDICO', 'Pago rechazado: el monto no coincide con la tarifa');
CALL sp_seed_cita('70000002',   3, '09:00', 'PROGRAMADA', 'POR_VALIDAR', NULL, NULL);
CALL sp_seed_cita('70000017',   3, '10:00', 'CANCELADA', NULL, 'MEDICO', 'Reserva duplicada por error del paciente');
CALL sp_seed_cita('70000020',   4, '09:00', 'PROGRAMADA', 'VALIDADO', NULL, NULL);
CALL sp_seed_cita('70000021',   5, '10:00', 'PROGRAMADA', 'VALIDADO', NULL, NULL);
CALL sp_seed_cita('70000022',   6, '11:00', 'PROGRAMADA', 'VALIDADO', NULL, NULL);
CALL sp_seed_cita('70000023',   7, '09:00', 'PROGRAMADA', 'POR_VALIDAR', NULL, NULL);
CALL sp_seed_cita('70000024',   8, '10:00', 'PROGRAMADA', 'POR_VALIDAR', NULL, NULL);
CALL sp_seed_cita('70000025',   9, '11:00', 'PROGRAMADA', 'POR_VALIDAR', NULL, NULL);
CALL sp_seed_cita('001000026', 10, '09:00', 'PROGRAMADA', 'POR_VALIDAR', NULL, NULL);

DROP PROCEDURE sp_seed_cita;

-- Resumen
SELECT estado, COUNT(*) AS citas FROM cita GROUP BY estado ORDER BY estado;
