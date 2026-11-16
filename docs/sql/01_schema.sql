-- =============================================================================
-- MEDIFAST — 01_schema.sql
-- Esquema de la base de datos (MySQL 8.4 LTS, InnoDB, utf8mb4).
-- Fuente: docs/diagramas/base-de-datos.md (BD-03, restricciones e índices).
--
-- Uso:  mysql -u root -p < docs/sql/01_schema.sql
-- El script es re-ejecutable: borra y vuelve a crear las tablas.
-- =============================================================================

CREATE DATABASE IF NOT EXISTS medifast
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE medifast;

SET FOREIGN_KEY_CHECKS = 0;
DROP VIEW  IF EXISTS v_cita;
DROP TABLE IF EXISTS pago;
DROP TABLE IF EXISTS cita_horario;
DROP TABLE IF EXISTS cita;
DROP TABLE IF EXISTS horario;
DROP TABLE IF EXISTS horario_semanal;
DROP TABLE IF EXISTS tarifa;
DROP TABLE IF EXISTS medico;
DROP TABLE IF EXISTS paciente;
DROP TABLE IF EXISTS usuario;
DROP TABLE IF EXISTS consultorio;
SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------------------------------
-- consultorio: parámetros del consultorio (multi-consultorio, RN-04, RN-06)
-- -----------------------------------------------------------------------------
CREATE TABLE consultorio (
    id_consultorio          BIGINT       NOT NULL AUTO_INCREMENT,
    nombre                  VARCHAR(100) NOT NULL,
    direccion               VARCHAR(200) NULL,
    dias_anticipacion_min   TINYINT      NOT NULL DEFAULT 1,
    dias_anticipacion_max   TINYINT      NOT NULL DEFAULT 30,
    minutos_bloque          TINYINT      NOT NULL DEFAULT 30,
    minutos_apartado        TINYINT      NOT NULL DEFAULT 15,
    activo                  BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_consultorio PRIMARY KEY (id_consultorio),
    CONSTRAINT uq_consultorio_nombre UNIQUE (nombre),
    CONSTRAINT ck_consultorio_anticipacion
        CHECK (dias_anticipacion_min >= 0 AND dias_anticipacion_max >= dias_anticipacion_min),
    CONSTRAINT ck_consultorio_minutos
        CHECK (minutos_bloque > 0 AND minutos_apartado > 0)
) ENGINE = InnoDB;

-- -----------------------------------------------------------------------------
-- usuario: credenciales de pacientes y médicos (RN-12)
-- -----------------------------------------------------------------------------
CREATE TABLE usuario (
    id_usuario          BIGINT                   NOT NULL AUTO_INCREMENT,
    tipo_documento      ENUM ('DNI', 'CE')       NOT NULL,
    numero_documento    VARCHAR(12)              NOT NULL,
    contrasena_hash     VARCHAR(60)              NOT NULL,
    rol                 ENUM ('PACIENTE', 'MEDICO') NOT NULL,
    activo              BOOLEAN                  NOT NULL DEFAULT TRUE,
    creado_en           DATETIME                 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en      DATETIME                 NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_usuario PRIMARY KEY (id_usuario),
    CONSTRAINT uq_usuario_documento UNIQUE (numero_documento),
    -- RF02: DNI de 8 dígitos; CE alfanumérico de 9 a 12 caracteres
    CONSTRAINT ck_usuario_documento CHECK (
        (tipo_documento = 'DNI' AND REGEXP_LIKE(numero_documento, '^[0-9]{8}$', 'c'))
        OR (tipo_documento = 'CE' AND REGEXP_LIKE(numero_documento, '^[0-9A-Z]{9,12}$', 'c'))
    )
) ENGINE = InnoDB;

-- -----------------------------------------------------------------------------
-- paciente: datos administrativos del paciente (RN-10)
-- -----------------------------------------------------------------------------
CREATE TABLE paciente (
    id_paciente         BIGINT       NOT NULL AUTO_INCREMENT,
    id_usuario          BIGINT       NOT NULL,
    nombres             VARCHAR(100) NOT NULL,
    apellidos           VARCHAR(100) NOT NULL,
    fecha_nacimiento    DATE         NOT NULL,
    celular             CHAR(9)      NOT NULL,
    correo              VARCHAR(150) NOT NULL,
    creado_en           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_paciente PRIMARY KEY (id_paciente),
    CONSTRAINT uq_paciente_usuario UNIQUE (id_usuario),
    CONSTRAINT uq_paciente_correo UNIQUE (correo),
    CONSTRAINT ck_paciente_celular CHECK (REGEXP_LIKE(celular, '^9[0-9]{8}$')),
    CONSTRAINT fk_paciente_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario) ON DELETE RESTRICT
) ENGINE = InnoDB;

-- -----------------------------------------------------------------------------
-- medico: se carga por script (no hay módulo de administración)
-- -----------------------------------------------------------------------------
CREATE TABLE medico (
    id_medico           BIGINT       NOT NULL AUTO_INCREMENT,
    id_usuario          BIGINT       NOT NULL,
    id_consultorio      BIGINT       NOT NULL,
    nombres             VARCHAR(100) NOT NULL,
    apellidos           VARCHAR(100) NOT NULL,
    cmp                 VARCHAR(10)  NOT NULL,
    especialidad        VARCHAR(100) NOT NULL,
    activo              BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_medico PRIMARY KEY (id_medico),
    CONSTRAINT uq_medico_usuario UNIQUE (id_usuario),
    CONSTRAINT uq_medico_cmp UNIQUE (cmp),
    CONSTRAINT fk_medico_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario) ON DELETE RESTRICT,
    CONSTRAINT fk_medico_consultorio FOREIGN KEY (id_consultorio)
        REFERENCES consultorio (id_consultorio) ON DELETE RESTRICT
) ENGINE = InnoDB;

-- -----------------------------------------------------------------------------
-- tarifa: duración y precio por tipo de cita y médico (RN-03)
-- -----------------------------------------------------------------------------
CREATE TABLE tarifa (
    id_tarifa           BIGINT                     NOT NULL AUTO_INCREMENT,
    id_medico           BIGINT                     NOT NULL,
    tipo_cita           ENUM ('NUEVO', 'CONTROL')  NOT NULL,
    duracion_min        SMALLINT                   NOT NULL,
    precio              DECIMAL(10, 2)             NOT NULL,
    creado_en           DATETIME                   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en      DATETIME                   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_tarifa PRIMARY KEY (id_tarifa),
    CONSTRAINT uq_tarifa_medico_tipo UNIQUE (id_medico, tipo_cita),
    CONSTRAINT ck_tarifa_valores CHECK (duracion_min > 0 AND precio > 0),
    CONSTRAINT fk_tarifa_medico FOREIGN KEY (id_medico)
        REFERENCES medico (id_medico) ON DELETE RESTRICT
) ENGINE = InnoDB;

-- -----------------------------------------------------------------------------
-- horario_semanal: plantilla de atención por médico (RN-08)
-- -----------------------------------------------------------------------------
CREATE TABLE horario_semanal (
    id_horario_semanal  BIGINT   NOT NULL AUTO_INCREMENT,
    id_medico           BIGINT   NOT NULL,
    dia_semana          TINYINT  NOT NULL,
    hora_inicio         TIME     NOT NULL,
    hora_fin            TIME     NOT NULL,
    creado_en           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_horario_semanal PRIMARY KEY (id_horario_semanal),
    CONSTRAINT ck_hs_dia CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT ck_hs_rango CHECK (hora_fin > hora_inicio),
    CONSTRAINT fk_hs_medico FOREIGN KEY (id_medico)
        REFERENCES medico (id_medico) ON DELETE RESTRICT
) ENGINE = InnoDB;

-- -----------------------------------------------------------------------------
-- horario: bloques de 30 min generados desde la plantilla semanal
-- -----------------------------------------------------------------------------
CREATE TABLE horario (
    id_horario          BIGINT   NOT NULL AUTO_INCREMENT,
    id_medico           BIGINT   NOT NULL,
    fecha               DATE     NOT NULL,
    hora_inicio         TIME     NOT NULL,
    hora_fin            TIME     NOT NULL,
    bloqueado           BOOLEAN  NOT NULL DEFAULT FALSE,
    creado_en           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_horario PRIMARY KEY (id_horario),
    CONSTRAINT uq_horario_medico_inicio UNIQUE (id_medico, fecha, hora_inicio),
    CONSTRAINT ck_horario_rango CHECK (hora_fin > hora_inicio),
    CONSTRAINT fk_horario_medico FOREIGN KEY (id_medico)
        REFERENCES medico (id_medico) ON DELETE RESTRICT
) ENGINE = InnoDB;

-- -----------------------------------------------------------------------------
-- cita: reserva de un paciente con un médico (RN-02, RN-05, RN-09)
-- -----------------------------------------------------------------------------
CREATE TABLE cita (
    id_cita             BIGINT       NOT NULL AUTO_INCREMENT,
    id_paciente         BIGINT       NOT NULL,
    id_medico           BIGINT       NOT NULL,
    tipo_cita           ENUM ('NUEVO', 'CONTROL') NOT NULL,
    estado              ENUM ('PENDIENTE_PAGO', 'PROGRAMADA', 'ATENDIDA', 'NO_ASISTIO', 'CANCELADA')
                                     NOT NULL DEFAULT 'PENDIENTE_PAGO',
    expira_en           DATETIME     NULL,
    observacion         VARCHAR(500) NULL,
    cancelada_por       ENUM ('SISTEMA', 'MEDICO') NULL,
    -- Inicio y fin de sus bloques; se fijan al reservar (rendimiento de v_cita)
    fecha               DATE         NOT NULL,
    hora_inicio         TIME         NOT NULL,
    hora_fin            TIME         NOT NULL,
    -- RN-09: vale id_paciente solo mientras la cita está activa; NULL en otro caso
    paciente_activo     BIGINT AS (IF(estado IN ('PENDIENTE_PAGO', 'PROGRAMADA'), id_paciente, NULL)) STORED,
    creado_en           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_cita PRIMARY KEY (id_cita),
    CONSTRAINT uq_cita_paciente_activo UNIQUE (paciente_activo),
    -- RN-13: toda cancelación lleva observación y responsable
    CONSTRAINT ck_cita_cancelacion CHECK (
        estado <> 'CANCELADA' OR (observacion IS NOT NULL AND cancelada_por IS NOT NULL)
    ),
    CONSTRAINT ck_cita_horas CHECK (hora_fin > hora_inicio),
    CONSTRAINT fk_cita_paciente FOREIGN KEY (id_paciente)
        REFERENCES paciente (id_paciente) ON DELETE RESTRICT,
    CONSTRAINT fk_cita_medico FOREIGN KEY (id_medico)
        REFERENCES medico (id_medico) ON DELETE RESTRICT,
    INDEX idx_cita_medico_estado (id_medico, estado),
    INDEX idx_cita_paciente_creado (id_paciente, creado_en),
    -- Agenda y reportes del médico; historial del paciente
    INDEX idx_cita_medico_fecha (id_medico, fecha, hora_inicio),
    INDEX idx_cita_paciente_fecha (id_paciente, fecha, hora_inicio),
    INDEX idx_cita_estado_expira (estado, expira_en)
) ENGINE = InnoDB;

-- -----------------------------------------------------------------------------
-- cita_horario: bloques que ocupa cada cita (1 = CONTROL, 2 = NUEVO) (RN-01)
-- -----------------------------------------------------------------------------
CREATE TABLE cita_horario (
    id_cita             BIGINT   NOT NULL,
    id_horario          BIGINT   NOT NULL,
    activo              BOOLEAN  NOT NULL DEFAULT TRUE,
    -- RN-01: vale id_horario solo mientras la reserva del bloque está activa
    horario_activo      BIGINT AS (IF(activo, id_horario, NULL)) STORED,
    creado_en           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_cita_horario PRIMARY KEY (id_cita, id_horario),
    CONSTRAINT uq_cita_horario_activo UNIQUE (horario_activo),
    CONSTRAINT fk_ch_cita FOREIGN KEY (id_cita)
        REFERENCES cita (id_cita) ON DELETE RESTRICT,
    CONSTRAINT fk_ch_horario FOREIGN KEY (id_horario)
        REFERENCES horario (id_horario) ON DELETE RESTRICT
) ENGINE = InnoDB;

-- -----------------------------------------------------------------------------
-- pago: pago simulado con Yape; se crea al adjuntar la constancia (RN-07, RN-11)
-- -----------------------------------------------------------------------------
CREATE TABLE pago (
    id_pago             BIGINT          NOT NULL AUTO_INCREMENT,
    id_cita             BIGINT          NOT NULL,
    metodo              VARCHAR(20)     NOT NULL DEFAULT 'YAPE_SIMULADO',
    monto               DECIMAL(10, 2)  NOT NULL,
    nro_operacion       VARCHAR(30)     NULL,
    comprobante_ruta    VARCHAR(255)    NOT NULL,
    comprobante_tipo    VARCHAR(50)     NOT NULL,
    estado              ENUM ('POR_VALIDAR', 'VALIDADO', 'RECHAZADO') NOT NULL DEFAULT 'POR_VALIDAR',
    fecha_pago          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    validado_en         DATETIME        NULL,
    creado_en           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_pago PRIMARY KEY (id_pago),
    CONSTRAINT uq_pago_cita UNIQUE (id_cita),
    CONSTRAINT ck_pago_monto CHECK (monto > 0),
    CONSTRAINT ck_pago_tipo CHECK (comprobante_tipo IN ('image/jpeg', 'image/png', 'application/pdf')),
    CONSTRAINT fk_pago_cita FOREIGN KEY (id_cita)
        REFERENCES cita (id_cita) ON DELETE RESTRICT,
    INDEX idx_pago_estado_fecha (estado, fecha_pago),
    -- Reporte de pagos (P10): filtra por fecha de validación
    INDEX idx_pago_estado_validado (estado, validado_en)
) ENGINE = InnoDB;

-- -----------------------------------------------------------------------------
-- v_cita: cita + paciente + médico + inicio/fin + pago
-- Sin tablas derivadas, para que MySQL la fusione con la consulta y use los
-- índices de cita.
-- -----------------------------------------------------------------------------
CREATE VIEW v_cita AS
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

-- -----------------------------------------------------------------------------
-- Usuario de aplicación con privilegios mínimos (README §11.2, seguridad).
-- Contraseña solo para desarrollo local: cámbiala fuera de tu máquina.
-- -----------------------------------------------------------------------------
CREATE USER IF NOT EXISTS 'medifast_app'@'%' IDENTIFIED BY 'medifast_app';
GRANT SELECT, INSERT, UPDATE, DELETE ON medifast.* TO 'medifast_app'@'%';
FLUSH PRIVILEGES;
