-- ============================================================
-- SCRIPT DDL + SEED FINAL v2.2 - REPOSITORIO CIRCULAR RECÍCLAME
-- Motor:   MySQL 8.0+
-- Stack:   Spring Data JPA + MySQL
-- ============================================================
-- CAMBIOS RESPECTO A v2.1
-- 1. usuarios.apellidos se divide en:
--    - apellido_paterno
--    - apellido_materno
--
-- 2. Se elimina usuarios.intentos_fallidos
--
-- 3. historial_roles.rol_anterior y rol_nuevo cambian a:
--    - id_rol_anterior
--    - id_rol_nuevo
--    ambos como FK a la tabla rol
--
-- 4. historial_roles.estado_anterior y estado_nuevo se modelan
--    como ENUM('ACTIVO','BLOQUEADO')
--    porque representan el estado de cuenta del usuario afectado,
--    no el estado del catálogo rol.
--
-- 5. Todas las columnas TEXT pasan a VARCHAR con longitudes razonables
--
-- 6. normativas.clasificacion cambia de VARCHAR a ENUM
--    para que en frontend se use como combobox
--
-- 7. notificaciones.tipo_entidad cambia de VARCHAR a ENUM
--
-- 8. Se elimina completamente la tabla metrica_sistema
-- ============================================================

DROP DATABASE IF EXISTS reciclame;
CREATE DATABASE reciclame
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE reciclame;

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- DROP TABLES
-- Orden inverso a dependencias
-- ============================================================
DROP TABLE IF EXISTS chat_mensaje;
DROP TABLE IF EXISTS chat_sesion;
DROP TABLE IF EXISTS actividad_sistema;
DROP TABLE IF EXISTS auditoria_acciones_admin;
DROP TABLE IF EXISTS historial_estudios;
DROP TABLE IF EXISTS historial_normativas;
DROP TABLE IF EXISTS historial_roles;
DROP TABLE IF EXISTS politica_contrasena;
DROP TABLE IF EXISTS dominio_autorizado;
DROP TABLE IF EXISTS configuracion_chatbot;
DROP TABLE IF EXISTS notificaciones;
DROP TABLE IF EXISTS registro_descarga;
DROP TABLE IF EXISTS normativa_categoria;
DROP TABLE IF EXISTS estudio_categoria;
DROP TABLE IF EXISTS categoria;
DROP TABLE IF EXISTS normativas;
DROP TABLE IF EXISTS estudios;
DROP TABLE IF EXISTS publicacion;
DROP TABLE IF EXISTS indicador_valor;
DROP TABLE IF EXISTS indicador;
DROP TABLE IF EXISTS registro_sesiones;
DROP TABLE IF EXISTS recuperacion_contrasena;
DROP TABLE IF EXISTS intento_login;
DROP TABLE IF EXISTS solicitud_registro;
DROP TABLE IF EXISTS usuario_empresa;
DROP TABLE IF EXISTS usuarios;
DROP TABLE IF EXISTS rol;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- TABLA 1: rol
-- Catálogo de roles del sistema
-- ============================================================
CREATE TABLE rol (
    id_rol         INT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre         VARCHAR(30)  NOT NULL COMMENT 'SUPERADMIN | ADMIN | SOCIO | VISUALIZADOR',
    descripcion    VARCHAR(200) NULL,
    estado         TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '1=activo 0=inactivo',
    fecha_creacion DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id_rol),
    UNIQUE KEY uq_rol_nombre (nombre)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Catálogo de roles del sistema.';

-- ============================================================
-- TABLA 2: usuarios
-- Se separa apellido paterno y apellido materno
-- Se elimina intentos_fallidos
-- ============================================================
CREATE TABLE usuarios (
    id_usuario         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_rol             INT UNSIGNED    NOT NULL COMMENT 'FK → rol',

    nombres            VARCHAR(100)    NOT NULL,
    apellido_paterno   VARCHAR(100)    NOT NULL,
    apellido_materno   VARCHAR(100)    NULL,

    dni                CHAR(8)         NOT NULL,
    correo             VARCHAR(150)    NOT NULL,
    telefono           VARCHAR(20)     NULL,
    contrasena_hash    VARCHAR(255)    NOT NULL COMMENT 'Hash bcrypt',
    url_avatar         VARCHAR(500)    NULL,

    estado_aprobacion  ENUM('PENDIENTE','APROBADO','RECHAZADO')
                                      NOT NULL DEFAULT 'PENDIENTE',

    estado_cuenta      ENUM('ACTIVO','BLOQUEADO')
                                      NULL COMMENT 'NULL mientras no esté aprobado',

    ultimo_acceso      DATETIME        NULL,
    fecha_registro     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    eliminado_en       DATETIME        NULL COMMENT 'Soft delete',

    PRIMARY KEY (id_usuario),
    UNIQUE KEY uq_usuarios_dni    (dni),
    UNIQUE KEY uq_usuarios_correo (correo),

    CONSTRAINT fk_usuarios_rol
        FOREIGN KEY (id_rol) REFERENCES rol(id_rol)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Tabla central de usuarios del sistema.';

-- ============================================================
-- TABLA 3: usuario_empresa
-- Datos empresariales opcionales del usuario
-- ============================================================
CREATE TABLE usuario_empresa (
    id_usuario_empresa BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_usuario         BIGINT UNSIGNED NOT NULL,
    ruc                CHAR(11)        NOT NULL,
    razon_social       VARCHAR(200)    NOT NULL,
    cargo              VARCHAR(100)    NULL,

    PRIMARY KEY (id_usuario_empresa),
    UNIQUE KEY uq_empresa_usuario (id_usuario),

    CONSTRAINT fk_empresa_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Datos corporativos opcionales por usuario.';

-- ============================================================
-- TABLA 4: solicitud_registro
-- También se separan los apellidos
-- ============================================================
CREATE TABLE solicitud_registro (
    id_solicitud      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    nombres           VARCHAR(100)    NOT NULL,
    apellido_paterno  VARCHAR(100)    NOT NULL,
    apellido_materno  VARCHAR(100)    NULL,
    dni               CHAR(8)         NOT NULL,
    correo            VARCHAR(150)    NOT NULL,
    telefono          VARCHAR(20)     NULL,
    ruc               CHAR(11)        NULL,

    rol_solicitado    ENUM('SOCIO','VISUALIZADOR') NOT NULL,

    estado            ENUM('PENDIENTE','APROBADO','RECHAZADO')
                                      NOT NULL DEFAULT 'PENDIENTE',

    motivo_rechazo    VARCHAR(1000)   NULL,

    revisado_por      BIGINT UNSIGNED NULL,
    id_usuario_creado BIGINT UNSIGNED NULL,

    fecha_solicitud   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_resolucion  DATETIME        NULL,

    PRIMARY KEY (id_solicitud),
    INDEX idx_solicitud_estado (estado, fecha_solicitud),

    CONSTRAINT fk_solicitud_revisado
        FOREIGN KEY (revisado_por) REFERENCES usuarios(id_usuario)
        ON DELETE SET NULL ON UPDATE CASCADE,

    CONSTRAINT fk_solicitud_usuario_creado
        FOREIGN KEY (id_usuario_creado) REFERENCES usuarios(id_usuario)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Snapshot del formulario de solicitud de registro.';

-- ============================================================
-- TABLA 5: intento_login
-- Log de intentos exitosos y fallidos
-- ============================================================
CREATE TABLE intento_login (
    id_intento  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_usuario  BIGINT UNSIGNED NULL,
    correo      VARCHAR(150)    NOT NULL,
    fecha       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip          VARCHAR(45)     NULL,
    exitoso     TINYINT(1)      NOT NULL DEFAULT 0,

    PRIMARY KEY (id_intento),
    INDEX idx_intento_usuario_fecha (id_usuario, fecha),
    INDEX idx_intento_correo_fecha  (correo, fecha),

    CONSTRAINT fk_intento_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Registro de intentos de inicio de sesión.';

-- ============================================================
-- TABLA 6: recuperacion_contrasena
-- ============================================================
CREATE TABLE recuperacion_contrasena (
    id_recuperacion  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_usuario       BIGINT UNSIGNED NOT NULL,
    codigo           VARCHAR(255)    NOT NULL,
    fecha_expiracion DATETIME        NOT NULL,
    usado            TINYINT(1)      NOT NULL DEFAULT 0,
    creado_en        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id_recuperacion),
    INDEX idx_recuperacion_usuario (id_usuario),

    CONSTRAINT fk_recuperacion_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Tokens o códigos de recuperación de contraseña.';

-- ============================================================
-- TABLA 7: registro_sesiones
-- ============================================================
CREATE TABLE registro_sesiones (
    id_sesion         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_usuario        BIGINT UNSIGNED NOT NULL,
    token             VARCHAR(255)    NULL,
    fecha_inicio      DATETIME        NOT NULL,
    fecha_fin         DATETIME        NULL,
    duracion_minutos  INT UNSIGNED    NULL,
    ip                VARCHAR(45)     NULL,
    agente_usuario    VARCHAR(500)    NULL,
    estado            ENUM('VIGENTE','FINALIZADA','EXPIRADA')
                                      NOT NULL DEFAULT 'VIGENTE',

    PRIMARY KEY (id_sesion),
    UNIQUE KEY uq_sesion_token (token),
    INDEX idx_sesion_usuario_fecha (id_usuario, fecha_inicio),

    CONSTRAINT fk_sesion_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Historial de sesiones del sistema.';

-- ============================================================
-- TABLA 8: categoria
-- ============================================================
CREATE TABLE categoria (
    id_categoria INT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre       VARCHAR(100) NOT NULL,
    descripcion  VARCHAR(200) NULL,
    color_hex    CHAR(7)      NULL,
    tipo         ENUM('ESTUDIO','NORMATIVA') NOT NULL,
    codigo       VARCHAR(10)  NULL,
    estado       TINYINT(1)   NOT NULL DEFAULT 1,

    PRIMARY KEY (id_categoria),
    UNIQUE KEY uq_categoria_nombre_tipo (nombre, tipo)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Catálogo de categorías para estudios y normativas.';

-- ============================================================
-- TABLA 9: estudios
-- TEXT reemplazado por VARCHAR
-- ============================================================
CREATE TABLE estudios (
    id_estudio          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    titulo              VARCHAR(300)    NOT NULL,
    descripcion         VARCHAR(2000)   NOT NULL,
    anio                YEAR            NOT NULL,
    formato             ENUM('PDF','PPTX') NOT NULL,
    estado              ENUM('VIGENTE','BORRADOR','DEROGADO')
                                        NOT NULL DEFAULT 'BORRADOR',
    tipo_acceso         ENUM('LECTURA','DESCARGA') NOT NULL,

    archivo_nombre      VARCHAR(255)    NULL,
    archivo_url         VARCHAR(500)    NULL,
    archivo_tamanio_kb  INT UNSIGNED    NULL,
    fecha_publicacion   DATE            NULL,
    indice_relevancia   TINYINT UNSIGNED NULL,
    informacion_legal   VARCHAR(1000)   NULL,

    id_usuario_creador  BIGINT UNSIGNED NOT NULL,
    id_usuario_editor   BIGINT UNSIGNED NULL,

    fecha_creacion      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    eliminado_en        DATETIME        NULL,

    PRIMARY KEY (id_estudio),
    INDEX idx_estudio_anio   (anio),
    INDEX idx_estudio_estado (estado),

    CONSTRAINT fk_estudio_creador
        FOREIGN KEY (id_usuario_creador) REFERENCES usuarios(id_usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT fk_estudio_editor
        FOREIGN KEY (id_usuario_editor) REFERENCES usuarios(id_usuario)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Repositorio de estudios.';

-- ============================================================
-- TABLA 10: estudio_categoria
-- ============================================================
CREATE TABLE estudio_categoria (
    id_estudio_categoria BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_estudio           BIGINT UNSIGNED NOT NULL,
    id_categoria         INT UNSIGNED    NOT NULL,

    PRIMARY KEY (id_estudio_categoria),
    UNIQUE KEY uq_estudio_categoria (id_estudio, id_categoria),

    CONSTRAINT fk_ec_estudio
        FOREIGN KEY (id_estudio) REFERENCES estudios(id_estudio)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT fk_ec_categoria
        FOREIGN KEY (id_categoria) REFERENCES categoria(id_categoria)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Tabla pivote M:N entre estudios y categorías.';

-- ============================================================
-- TABLA 11: normativas
-- clasificacion cambia a ENUM para usar combobox
-- TEXT reemplazado por VARCHAR
-- ============================================================
CREATE TABLE normativas (
    id_normativa        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    titulo              VARCHAR(400)    NOT NULL,
    descripcion         VARCHAR(2000)   NULL,
    codigo              VARCHAR(50)     NULL,
    organismo_emisor    VARCHAR(200)    NOT NULL,
    anio                YEAR            NOT NULL,

    tipo_norma          ENUM('LEY_NACIONAL','DECRETO_SUPREMO','REGLAMENTO',
                             'ANTEPROYECTO','HOJA_DE_RUTA','DECRETO_LEY','OTRO')
                                        NOT NULL,

    estado              ENUM('VIGENTE','DEROGADA','PUBLICADA',
                             'CONSULTA_PUBLICA','BORRADOR_EN_PROCESO')
                                        NOT NULL,

    acceso              ENUM('GRATIS','PAGO') NOT NULL,
    alcance             ENUM('NACIONAL','INTERNACIONAL') NOT NULL,

    campo_aplicacion    VARCHAR(1000)   NULL,

    -- Este ENUM permite mapear directamente a un combo/select en frontend
    clasificacion       ENUM('PRIORIDAD_ALTA','PRIORIDAD_MEDIA','PRIORIDAD_BAJA',
                             'REFERENCIA_TECNICA','PLANEAMIENTO','INNOVACION_NORMATIVA',
                             'OBLIGATORIA','COMPLEMENTARIA')
                                        NULL,

    obligatoriedad      VARCHAR(100)    NULL,

    archivo_nombre      VARCHAR(255)    NULL,
    archivo_url         VARCHAR(500)    NULL,
    enlace_externo      VARCHAR(500)    NULL,

    id_usuario_creador  BIGINT UNSIGNED NOT NULL,
    id_usuario_editor   BIGINT UNSIGNED NULL,

    fecha_creacion      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    eliminado_en        DATETIME        NULL,

    PRIMARY KEY (id_normativa),
    INDEX idx_normativa_alcance_estado (alcance, estado),
    INDEX idx_normativa_anio (anio),

    CONSTRAINT chk_pago_sin_archivo
        CHECK (NOT (acceso = 'PAGO' AND archivo_url IS NOT NULL)),

    CONSTRAINT chk_gratis_necesita_acceso
        CHECK (NOT (acceso = 'GRATIS' AND archivo_url IS NULL AND enlace_externo IS NULL)),

    CONSTRAINT fk_normativa_creador
        FOREIGN KEY (id_usuario_creador) REFERENCES usuarios(id_usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT fk_normativa_editor
        FOREIGN KEY (id_usuario_editor) REFERENCES usuarios(id_usuario)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Repositorio normativo.';

-- ============================================================
-- TABLA 12: normativa_categoria
-- ============================================================
CREATE TABLE normativa_categoria (
    id_normativa_categoria BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_normativa           BIGINT UNSIGNED NOT NULL,
    id_categoria           INT UNSIGNED    NOT NULL,

    PRIMARY KEY (id_normativa_categoria),
    UNIQUE KEY uq_normativa_categoria (id_normativa, id_categoria),

    CONSTRAINT fk_nc_normativa
        FOREIGN KEY (id_normativa) REFERENCES normativas(id_normativa)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT fk_nc_categoria
        FOREIGN KEY (id_categoria) REFERENCES categoria(id_categoria)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Tabla pivote M:N entre normativas y categorías.';

-- ============================================================
-- TABLA 13: registro_descarga
-- ============================================================
CREATE TABLE registro_descarga (
    id_descarga      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_usuario       BIGINT UNSIGNED NOT NULL,
    tipo_documento   ENUM('ESTUDIO','NORMATIVA') NOT NULL,
    id_documento     BIGINT UNSIGNED NOT NULL,
    nombre_documento VARCHAR(400)    NOT NULL,
    url_descargada   VARCHAR(500)    NOT NULL,
    fecha_descarga   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id_descarga),
    INDEX idx_descarga_usuario_fecha  (id_usuario, fecha_descarga),
    INDEX idx_descarga_tipo_documento (tipo_documento, id_documento),

    CONSTRAINT fk_descarga_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Historial de descargas realizadas.';

-- ============================================================
-- TABLA 14: notificaciones
-- tipo_entidad cambia a ENUM
-- mensaje deja de ser TEXT y pasa a VARCHAR
-- ============================================================
CREATE TABLE notificaciones (
    id_notificacion   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_usuario        BIGINT UNSIGNED NOT NULL,
    titulo            VARCHAR(200)    NOT NULL,
    mensaje           VARCHAR(1000)   NOT NULL,
    tipo              ENUM('SISTEMA','ESTUDIO','NORMATIVA','SOLICITUD') NOT NULL,
    leido             TINYINT(1)      NOT NULL DEFAULT 0,
    enlace_referencia VARCHAR(500)    NULL,

    -- Este ENUM sirve como selector controlado desde backend/frontend
    tipo_entidad      ENUM('USUARIO','ESTUDIO','NORMATIVA','PUBLICACION',
                           'SOLICITUD_REGISTRO','AUDITORIA','CONFIGURACION')
                                      NULL,

    id_entidad        BIGINT UNSIGNED NULL,
    fecha             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id_notificacion),
    INDEX idx_notificacion_usuario_leido (id_usuario, leido, fecha),

    CONSTRAINT fk_notificacion_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Notificaciones dirigidas a usuarios específicos.';

-- ============================================================
-- TABLA 15: indicador
-- ============================================================
CREATE TABLE indicador (
    id_indicador INT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre       VARCHAR(200) NOT NULL,
    descripcion  VARCHAR(500) NULL,
    categoria    VARCHAR(100) NULL,
    unidad       VARCHAR(50)  NULL,
    estado       TINYINT(1)   NOT NULL DEFAULT 1,

    PRIMARY KEY (id_indicador)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Catálogo de indicadores.';

-- ============================================================
-- TABLA 16: indicador_valor
-- ============================================================
CREATE TABLE indicador_valor (
    id_valor       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_indicador   INT UNSIGNED    NOT NULL,
    valor          DECIMAL(15,4)   NOT NULL,
    anio           YEAR            NOT NULL,
    fecha_registro DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id_valor),
    INDEX idx_indicador_valor_anio (id_indicador, anio),

    CONSTRAINT fk_valor_indicador
        FOREIGN KEY (id_indicador) REFERENCES indicador(id_indicador)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Valores históricos por indicador.';

-- ============================================================
-- TABLA 17: publicacion
-- contenido pasa a VARCHAR
-- ============================================================
CREATE TABLE publicacion (
    id_publicacion     INT UNSIGNED NOT NULL AUTO_INCREMENT,
    titulo             VARCHAR(300) NOT NULL,
    contenido          VARCHAR(2000) NULL,
    tipo               ENUM('NEWSLETTER','EDITORIAL') NOT NULL,
    archivo_nombre     VARCHAR(255) NULL,
    archivo_url        VARCHAR(500) NULL,
    fecha_publicacion  DATE         NOT NULL,
    id_usuario_creador BIGINT UNSIGNED NOT NULL,
    creado_en          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    eliminado_en       DATETIME     NULL,

    PRIMARY KEY (id_publicacion),

    CONSTRAINT fk_publicacion_creador
        FOREIGN KEY (id_usuario_creador) REFERENCES usuarios(id_usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Publicaciones institucionales.';

-- ============================================================
-- TABLA 18: actividad_sistema
-- ============================================================
CREATE TABLE actividad_sistema (
    id_actividad  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    tipo          ENUM('ESTUDIO','NORMATIVA','USUARIO','INDICADOR','PUBLICACION') NOT NULL,
    id_referencia BIGINT UNSIGNED NULL,
    descripcion   VARCHAR(500)    NOT NULL,
    accion        ENUM('AÑADIDO','MODIFICADO','ELIMINADO') NOT NULL,
    fecha         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id_actividad),
    INDEX idx_actividad_fecha (fecha)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Feed global de actividad reciente.';

-- ============================================================
-- TABLA 19: auditoria_acciones_admin
-- descripcion pasa a VARCHAR
-- ============================================================
CREATE TABLE auditoria_acciones_admin (
    id_auditoria     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_usuario       BIGINT UNSIGNED NOT NULL,
    accion           VARCHAR(30)     NOT NULL,
    modulo           VARCHAR(50)     NOT NULL,
    id_entidad       BIGINT UNSIGNED NULL,
    descripcion      VARCHAR(2000)   NOT NULL,
    datos_anteriores JSON            NULL,
    datos_nuevos     JSON            NULL,
    fecha            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip               VARCHAR(45)     NULL,

    PRIMARY KEY (id_auditoria),
    INDEX idx_auditoria_usuario_fecha (id_usuario, fecha),
    INDEX idx_auditoria_modulo_fecha  (modulo, fecha),

    CONSTRAINT fk_auditoria_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Log inmutable de acciones administrativas.';

-- ============================================================
-- TABLA 20: historial_estudios
-- valores cambian de TEXT a VARCHAR
-- ============================================================
CREATE TABLE historial_estudios (
    id_historial      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_estudio        BIGINT UNSIGNED NOT NULL,
    campo_modificado  VARCHAR(100)    NOT NULL,
    valor_anterior    VARCHAR(2000)   NULL,
    valor_nuevo       VARCHAR(2000)   NULL,
    modificado_por    BIGINT UNSIGNED NOT NULL,
    es_rollback       TINYINT(1)      NOT NULL DEFAULT 0,
    id_version_origen BIGINT UNSIGNED NULL,
    fecha_cambio      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id_historial),
    INDEX idx_hist_est_estudio (id_estudio, fecha_cambio),
    INDEX idx_hist_est_admin   (modificado_por, fecha_cambio),

    CONSTRAINT fk_hist_est_estudio
        FOREIGN KEY (id_estudio) REFERENCES estudios(id_estudio)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT fk_hist_est_admin
        FOREIGN KEY (modificado_por) REFERENCES usuarios(id_usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT fk_hist_est_version
        FOREIGN KEY (id_version_origen) REFERENCES historial_estudios(id_historial)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Historial de cambios campo a campo de estudios.';

-- ============================================================
-- TABLA 21: historial_normativas
-- valores cambian de TEXT a VARCHAR
-- ============================================================
CREATE TABLE historial_normativas (
    id_historial      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_normativa      BIGINT UNSIGNED NOT NULL,
    campo_modificado  VARCHAR(100)    NOT NULL,
    valor_anterior    VARCHAR(2000)   NULL,
    valor_nuevo       VARCHAR(2000)   NULL,
    modificado_por    BIGINT UNSIGNED NOT NULL,
    es_rollback       TINYINT(1)      NOT NULL DEFAULT 0,
    id_version_origen BIGINT UNSIGNED NULL,
    fecha_cambio      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id_historial),
    INDEX idx_hist_norm_normativa (id_normativa, fecha_cambio),
    INDEX idx_hist_norm_admin     (modificado_por, fecha_cambio),

    CONSTRAINT fk_hist_norm_normativa
        FOREIGN KEY (id_normativa) REFERENCES normativas(id_normativa)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT fk_hist_norm_admin
        FOREIGN KEY (modificado_por) REFERENCES usuarios(id_usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT fk_hist_norm_version
        FOREIGN KEY (id_version_origen) REFERENCES historial_normativas(id_historial)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Historial de cambios campo a campo de normativas.';

-- ============================================================
-- TABLA 22: historial_roles
-- rol_anterior/rol_nuevo ahora son FKs a rol
-- estado_anterior/estado_nuevo se enumeran
-- ============================================================
CREATE TABLE historial_roles (
    id_historial_rol    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_usuario_afectado BIGINT UNSIGNED NOT NULL,

    id_rol_anterior     INT UNSIGNED    NULL,
    id_rol_nuevo        INT UNSIGNED    NULL,

    -- Se usa ENUM porque estos estados son del usuario,
    -- específicamente de usuarios.estado_cuenta.
    estado_anterior     ENUM('ACTIVO','BLOQUEADO') NULL,
    estado_nuevo        ENUM('ACTIVO','BLOQUEADO') NULL,

    autorizado_por      BIGINT UNSIGNED NOT NULL,
    motivo              VARCHAR(1000)   NULL,
    fecha_cambio        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id_historial_rol),
    INDEX idx_historial_rol_usuario (id_usuario_afectado, fecha_cambio),
    INDEX idx_historial_rol_admin   (autorizado_por, fecha_cambio),

    CONSTRAINT fk_historial_rol_afectado
        FOREIGN KEY (id_usuario_afectado) REFERENCES usuarios(id_usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT fk_historial_rol_autorizado
        FOREIGN KEY (autorizado_por) REFERENCES usuarios(id_usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT fk_historial_rol_rol_anterior
        FOREIGN KEY (id_rol_anterior) REFERENCES rol(id_rol)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT fk_historial_rol_rol_nuevo
        FOREIGN KEY (id_rol_nuevo) REFERENCES rol(id_rol)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Historial de cambios de rol y estado de cuenta.';

-- ============================================================
-- TABLA 23: dominio_autorizado
-- ============================================================
CREATE TABLE dominio_autorizado (
    id_dominio          INT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre_dominio      VARCHAR(100) NOT NULL,
    motivo_autorizacion VARCHAR(500) NULL,
    estado              TINYINT(1)   NOT NULL DEFAULT 1,
    id_usuario_creador  BIGINT UNSIGNED NOT NULL,
    fecha_registro      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id_dominio),
    UNIQUE KEY uq_dominio (nombre_dominio),

    CONSTRAINT fk_dominio_creador
        FOREIGN KEY (id_usuario_creador) REFERENCES usuarios(id_usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Lista blanca de dominios autorizados.';

-- ============================================================
-- TABLA 24: politica_contrasena
-- ============================================================
CREATE TABLE politica_contrasena (
    id_politica        TINYINT UNSIGNED NOT NULL,
    longitud_minima    TINYINT UNSIGNED NOT NULL DEFAULT 8,
    requiere_mayuscula TINYINT(1)       NOT NULL DEFAULT 1,
    requiere_numero    TINYINT(1)       NOT NULL DEFAULT 1,
    requiere_simbolo   TINYINT(1)       NOT NULL DEFAULT 1,
    mandato_mfa        TINYINT(1)       NOT NULL DEFAULT 0,
    expiracion_dias    SMALLINT UNSIGNED NULL,
    actualizado_por    BIGINT UNSIGNED  NOT NULL,
    actualizado_en     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id_politica),

    CONSTRAINT fk_politica_actualizado
        FOREIGN KEY (actualizado_por) REFERENCES usuarios(id_usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Configuración central de política de contraseñas.';

-- ============================================================
-- TABLA 25: configuracion_chatbot
-- metrica_sistema fue eliminada, así que esta pasa a ser 25
-- ============================================================
CREATE TABLE configuracion_chatbot (
    id_chatbot            TINYINT UNSIGNED NOT NULL,
    activo                TINYINT(1)       NOT NULL DEFAULT 1,
    proveedor             VARCHAR(100)     NULL,
    tasa_resolucion_pct   DECIMAL(5,2)     NULL,
    tiempo_respuesta_seg  DECIMAL(5,2)     NULL,
    consultas_hoy         INT UNSIGNED     NOT NULL DEFAULT 0,
    actualizado_por       BIGINT UNSIGNED  NOT NULL,
    actualizado_en        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id_chatbot),

    CONSTRAINT fk_chatbot_actualizado
        FOREIGN KEY (actualizado_por) REFERENCES usuarios(id_usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Configuración principal del chatbot.';

-- ============================================================
-- TABLA 26: chat_sesion
-- ============================================================
CREATE TABLE chat_sesion (
    id_chat      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_usuario   BIGINT UNSIGNED NOT NULL,
    fecha_inicio DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_fin    DATETIME        NULL,
    activa       TINYINT(1)      NOT NULL DEFAULT 1,

    PRIMARY KEY (id_chat),
    INDEX idx_chat_usuario (id_usuario),

    CONSTRAINT fk_chat_sesion_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Sesiones del chatbot por usuario.';

-- ============================================================
-- TABLA 27: chat_mensaje
-- mensaje pasa a VARCHAR
-- ============================================================
CREATE TABLE chat_mensaje (
    id_mensaje BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_chat    BIGINT UNSIGNED NOT NULL,
    emisor     ENUM('USUARIO','SISTEMA') NOT NULL,
    mensaje    VARCHAR(2000)   NOT NULL,
    fecha      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id_mensaje),
    INDEX idx_chat_mensaje (id_chat, fecha),

    CONSTRAINT fk_mensaje_chat
        FOREIGN KEY (id_chat) REFERENCES chat_sesion(id_chat)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Mensajes individuales por sesión de chatbot.';

-- ============================================================
-- DATOS INICIALES
-- ============================================================

START TRANSACTION;

-- ------------------------------------------------------------
-- ROLES
-- ------------------------------------------------------------
INSERT INTO rol (id_rol, nombre, descripcion, estado) VALUES
(1, 'SUPERADMIN',   'Control total del sistema y auditoría', 1),
(2, 'ADMIN',        'Gestión operativa del repositorio y usuarios', 1),
(3, 'SOCIO',        'Acceso con descarga según permisos', 1),
(4, 'VISUALIZADOR', 'Acceso de solo lectura', 1);

-- ------------------------------------------------------------
-- CATEGORÍAS
-- ------------------------------------------------------------
INSERT INTO categoria (id_categoria, nombre, descripcion, color_hex, tipo, codigo, estado) VALUES
(1,  'Economía Circular',         'Normativas de economía circular',              '#3CC68A', 'NORMATIVA', 'EC',   1),
(2,  'Gestión de Residuos',       'Normativas de gestión y tratamiento',          '#378ADD', 'NORMATIVA', 'GR',   1),
(3,  'Envases y Embalajes',     'Normativas relacionadas a envases y embalajes',  '#F59E0B', 'NORMATIVA', 'EE',   1),
(4,  'Responsabilidad Extendida', 'Normativas REP del productor',                 '#EF4444', 'NORMATIVA', 'REP',  1),
(5,  'Otro',                      'Otras temáticas normativas',                   '#888780', 'NORMATIVA', 'OTRO', 1),
(6,  'Mercado de residuos',       'Estudios sobre oferta y demanda de residuos',  '#2F855A', 'ESTUDIO',   'MR',   1),
(7,  'Economía circular aplicada','Estudios aplicados al cierre de ciclos',       '#3182CE', 'ESTUDIO',   'ECA',  1),
(8,  'Trazabilidad',              'Monitoreo y seguimiento documental',           '#805AD5', 'ESTUDIO',   'TRZ',  1),
(9,  'Innovación y tecnología',   'Transformación digital y valorización',        '#DD6B20', 'ESTUDIO',   'IT',   1),
(10, 'Política pública',          'Estudios de soporte a regulación',             '#E53E3E', 'ESTUDIO',   'PP',   1);

-- ------------------------------------------------------------
-- USUARIOS
-- ------------------------------------------------------------
INSERT INTO usuarios
(id_usuario, id_rol, nombres, apellido_paterno, apellido_materno, dni, correo, telefono, contrasena_hash, url_avatar,
 estado_aprobacion, estado_cuenta, ultimo_acceso, fecha_registro, actualizado_en, eliminado_en)
VALUES
(1, 1, 'Adrian',     'Moreno',  'Soto',   '72643574', 'adrian.moreno@reciclame.pe',      '999111111', '$2a$10$HD..tOqvIDKnRxRTPamcrOtp.IUyB8Dx9ys/jaZ2LY79l2O.Qk/A.', NULL, 'APROBADO', 'ACTIVO',    '2026-04-15 09:05:00', '2026-03-25 08:00:00', '2026-04-15 09:05:00', NULL),
(2, 2, 'Matthew', 'Salvador', 'Sotelo', '20222079', 'a20222079@pucp.edu.pe', '999222222', '$2a$10$HD..tOqvIDKnRxRTPamcrOtp.IUyB8Dx9ys/jaZ2LY79l2O.Qk/A.', NULL, 'APROBADO', 'ACTIVO', '2026-04-15 08:40:00', '2026-03-26 09:30:00', '2026-04-15 08:40:00', NULL),
(3, 2, 'Mariana',   'Huertas',   'Oliva',   '74567890', 'mariana.huertas@reciclame.pe',     '999333333', '$2a$10$HD..tOqvIDKnRxRTPamcrOtp.IUyB8Dx9ys/jaZ2LY79l2O.Qk/A.',      NULL, 'APROBADO', 'ACTIVO',    '2026-04-14 18:22:00', '2026-03-27 10:15:00', '2026-04-14 18:22:00', NULL),
(4, 3, 'Marco',   'Silva',   'Perez',  '75678901', 'marco.silva@alicorp.com',      '999444444', '$2a$10$HD..tOqvIDKnRxRTPamcrOtp.IUyB8Dx9ys/jaZ2LY79l2O.Qk/A.',      NULL, 'APROBADO', 'ACTIVO',    '2026-04-15 07:55:00', '2026-03-29 11:00:00', '2026-04-15 07:55:00', NULL),
(5, 3, 'Paola',   'Flores',  'Mendez', '76789012', 'paola.flores@cicla.pe',        '999555555', '$2a$10$HD..tOqvIDKnRxRTPamcrOtp.IUyB8Dx9ys/jaZ2LY79l2O.Qk/A.',      NULL, 'APROBADO', 'ACTIVO',    '2026-04-14 20:10:00', '2026-03-30 14:20:00', '2026-04-14 20:10:00', NULL),
(6, 3, 'Roberto', 'Bravo',   'Castro', '77890123', 'roberto.bravo@greenloop.pe',   '999666666', '$2a$10$HD..tOqvIDKnRxRTPamcrOtp.IUyB8Dx9ys/jaZ2LY79l2O.Qk/A.',      NULL, 'APROBADO', 'BLOQUEADO', '2026-04-10 16:10:00', '2026-03-31 09:45:00', '2026-04-10 16:10:00', NULL),
(7, 4, 'Elena',   'Torres',  'Ruiz',   '78901234', 'elena.torres@upn.edu.pe',      '999777777', '$2a$10$HD..tOqvIDKnRxRTPamcrOtp.IUyB8Dx9ys/jaZ2LY79l2O.Qk/A.',     NULL, 'APROBADO', 'ACTIVO',    '2026-04-15 09:11:00', '2026-04-01 08:50:00', '2026-04-15 09:11:00', NULL),
(8, 4, 'Diego',   'Mendoza', 'Salas',  '79012345', 'diego.mendoza@pucp.edu.pe',    '999888888', '$2a$10$HD..tOqvIDKnRxRTPamcrOtp.IUyB8Dx9ys/jaZ2LY79l2O.Qk/A.',     NULL, 'APROBADO', 'ACTIVO',    '2026-04-13 17:00:00', '2026-04-02 17:10:00', '2026-04-13 17:00:00', NULL),
(9, 3, 'Rosa',    'Vega',    'Rios',   '70123456', 'rosa.vega@ecoperu.pe',         '999123123', '$2a$10$pendinghashdemo9',    NULL, 'PENDIENTE', NULL,        NULL,                  '2026-04-05 12:00:00', '2026-04-05 12:00:00', NULL),
(10,4, 'Jorge',   'Paredes', 'Campos', '71234567', 'jorge.paredes@viewer.pe',      '999234234', '$2a$10$rechashdemo10',       NULL, 'RECHAZADO', NULL,        NULL,                  '2026-04-03 16:30:00', '2026-04-06 09:00:00', NULL),
(11,3, 'Andrea',  'Vargas',  'Peña',   '72345671', 'andrea.vargas@alicorp.com',    '988111111', '$2a$10$hash11',               NULL, 'APROBADO', 'ACTIVO',    '2026-04-16 08:10:00', '2026-04-06 08:00:00', '2026-04-16 08:10:00', NULL),
(12,3, 'Miguel',  'Cruz',    'Luna',   '72345672', 'miguel.cruz@greenloop.pe',     '988111112', '$2a$10$hash12',               NULL, 'APROBADO', 'ACTIVO',    '2026-04-16 09:10:00', '2026-04-06 09:00:00', '2026-04-16 09:10:00', NULL),
(13,4, 'Sofia',   'Huaman',  'Rojas',  '72345673', 'sofia.huaman@upn.edu.pe',      '988111113', '$2a$10$hash13',               NULL, 'APROBADO', 'ACTIVO',    '2026-04-16 10:10:00', '2026-04-07 09:00:00', '2026-04-16 10:10:00', NULL),
(14,4, 'Pedro',   'Navarro', 'Leon',   '72345674', 'pedro.navarro@pucp.edu.pe',    '988111114', '$2a$10$hash14',               NULL, 'APROBADO', 'ACTIVO',    '2026-04-16 10:40:00', '2026-04-07 11:00:00', '2026-04-16 10:40:00', NULL),
(15,3, 'Valeria', 'Suarez',  'Gil',    '72345675', 'valeria.suarez@cicla.pe',      '988111115', '$2a$10$hash15',               NULL, 'APROBADO', 'ACTIVO',    '2026-04-16 11:15:00', '2026-04-08 08:00:00', '2026-04-16 11:15:00', NULL),
(16,4, 'Luis',    'Quispe',  'Mora',   '72345676', 'luis.quispe@upn.edu.pe',       '988111116', '$2a$10$hash16',               NULL, 'APROBADO', 'ACTIVO',    '2026-04-16 12:05:00', '2026-04-08 10:00:00', '2026-04-16 12:05:00', NULL),
(17,3, 'Camila',  'Rojas',   'Tapia',  '72345677', 'camila.rojas@ecoperu.pe',      '988111117', '$2a$10$hash17',               NULL, 'PENDIENTE', NULL,        NULL,                  '2026-04-09 12:00:00', '2026-04-09 12:00:00', NULL),
(18,4, 'Alvaro',  'Molina',  'Sosa',   '72345678', 'alvaro.molina@viewer.pe',      '988111118', '$2a$10$hash18',               NULL, 'RECHAZADO', NULL,        NULL,                  '2026-04-10 15:00:00', '2026-04-10 15:00:00', NULL);

-- ------------------------------------------------------------
-- USUARIO_EMPRESA
-- ------------------------------------------------------------
INSERT INTO usuario_empresa
(id_usuario_empresa, id_usuario, ruc, razon_social, cargo)
VALUES
(1, 4,  '20100070970', 'Alicorp S.A.A.',            'Coordinador de sostenibilidad'),
(2, 5,  '20604561234', 'Cicla Perú S.A.C.',         'Analista de economía circular'),
(3, 6,  '20607894561', 'GreenLoop Consultores',     'Jefe de operaciones'),
(4, 9,  '20599887766', 'EcoPerú Innovación S.A.C.', 'Especialista ambiental'),
(5, 11, '20100070970', 'Alicorp S.A.A.',            'Analista de proyectos'),
(6, 12, '20607894561', 'GreenLoop Consultores',     'Consultor senior'),
(7, 15, '20604561234', 'Cicla Perú S.A.C.',         'Especialista de cumplimiento'),
(8, 17, '20599887766', 'EcoPerú Innovación S.A.C.', 'Postulante');

-- ------------------------------------------------------------
-- SOLICITUD_REGISTRO
-- ------------------------------------------------------------
INSERT INTO solicitud_registro
(id_solicitud, nombres, apellido_paterno, apellido_materno, dni, correo, telefono, ruc, rol_solicitado, estado, motivo_rechazo,
 revisado_por, id_usuario_creado, fecha_solicitud, fecha_resolucion)
VALUES
(1, 'Marco',   'Silva',   'Perez',  '75678901', 'marco.silva@alicorp.com',   '999444444', '20100070970', 'SOCIO',        'APROBADO',  NULL, 2, 4,  '2026-03-28 10:10:00', '2026-03-29 10:00:00'),
(2, 'Paola',   'Flores',  'Mendez', '76789012', 'paola.flores@cicla.pe',     '999555555', '20604561234', 'SOCIO',        'APROBADO',  NULL, 3, 5,  '2026-03-29 11:30:00', '2026-03-30 14:00:00'),
(3, 'Elena',   'Torres',  'Ruiz',   '78901234', 'elena.torres@upn.edu.pe',   '999777777', NULL,          'VISUALIZADOR', 'APROBADO',  NULL, 2, 7,  '2026-03-31 08:30:00', '2026-04-01 08:30:00'),
(4, 'Jorge',   'Paredes', 'Campos', '71234567', 'jorge.paredes@viewer.pe',   '999234234', NULL,          'VISUALIZADOR', 'RECHAZADO', 'El dominio de correo no está autorizado.', 3, NULL, '2026-04-03 15:00:00', '2026-04-06 09:00:00'),
(5, 'Rosa',    'Vega',    'Rios',   '70123456', 'rosa.vega@ecoperu.pe',      '999123123', '20599887766', 'SOCIO',        'PENDIENTE', NULL, NULL, NULL, '2026-04-05 12:00:00', NULL),
(6, 'Andrea',  'Vargas',  'Peña',   '72345671', 'andrea.vargas@alicorp.com', '988111111', '20100070970', 'SOCIO',        'APROBADO',  NULL, 2, 11, '2026-04-06 08:00:00', '2026-04-06 10:00:00'),
(7, 'Miguel',  'Cruz',    'Luna',   '72345672', 'miguel.cruz@greenloop.pe',  '988111112', '20607894561', 'SOCIO',        'APROBADO',  NULL, 3, 12, '2026-04-06 09:00:00', '2026-04-06 11:00:00'),
(8, 'Sofia',   'Huaman',  'Rojas',  '72345673', 'sofia.huaman@upn.edu.pe',   '988111113', NULL,          'VISUALIZADOR', 'APROBADO',  NULL, 2, 13, '2026-04-07 09:00:00', '2026-04-07 12:00:00'),
(9, 'Pedro',   'Navarro', 'Leon',   '72345674', 'pedro.navarro@pucp.edu.pe', '988111114', NULL,          'VISUALIZADOR', 'APROBADO',  NULL, 2, 14, '2026-04-07 11:00:00', '2026-04-07 13:00:00'),
(10,'Valeria', 'Suarez',  'Huertas',    '72345675', 'valeria.suarez@cicla.pe',   '988111115', '20604561234', 'SOCIO',        'APROBADO',  NULL, 3, 15, '2026-04-08 08:00:00', '2026-04-08 10:00:00');

-- ------------------------------------------------------------
-- INTENTO_LOGIN
-- ------------------------------------------------------------
INSERT INTO intento_login
(id_intento, id_usuario, correo, fecha, ip, exitoso)
VALUES
(1, 4, 'marco.silva@alicorp.com',    '2026-04-15 07:55:00', '190.12.10.1', 1),
(2, 5, 'paola.flores@cicla.pe',      '2026-04-14 20:10:00', '190.12.10.2', 1),
(3, 6, 'roberto.bravo@greenloop.pe', '2026-04-10 15:59:00', '190.12.10.3', 0),
(4, 6, 'roberto.bravo@greenloop.pe', '2026-04-10 16:01:00', '190.12.10.3', 0),
(5, 6, 'roberto.bravo@greenloop.pe', '2026-04-10 16:03:00', '190.12.10.3', 0),
(6, 6, 'roberto.bravo@greenloop.pe', '2026-04-10 16:05:00', '190.12.10.3', 0),
(7, 6, 'roberto.bravo@greenloop.pe', '2026-04-10 16:07:00', '190.12.10.3', 0),
(8, 7, 'elena.torres@upn.edu.pe',    '2026-04-15 09:11:00', '190.12.10.4', 1),
(9, NULL, 'inexistente@falso.pe',    '2026-04-11 11:05:00', '190.12.10.9', 0),
(10,8, 'diego.mendoza@pucp.edu.pe',  '2026-04-13 17:00:00', '190.12.10.5', 1),
(11,11,'andrea.vargas@alicorp.com',  '2026-04-16 08:10:00', '190.12.10.6', 1),
(12,12,'miguel.cruz@greenloop.pe',   '2026-04-16 09:10:00', '190.12.10.7', 1),
(13,13,'sofia.huaman@upn.edu.pe',    '2026-04-16 10:10:00', '190.12.10.8', 1),
(14,14,'pedro.navarro@pucp.edu.pe',  '2026-04-16 10:40:00', '190.12.10.10',1);

-- ------------------------------------------------------------
-- RECUPERACION_CONTRASENA
-- ------------------------------------------------------------
INSERT INTO recuperacion_contrasena
(id_recuperacion, id_usuario, codigo, fecha_expiracion, usado, creado_en)
VALUES
(1, 4,  'OTP-MARCO-001',  '2026-04-10 10:30:00', 1, '2026-04-10 10:00:00'),
(2, 5,  'OTP-PAOLA-002',  '2026-04-12 19:30:00', 0, '2026-04-12 19:00:00'),
(3, 7,  'OTP-ELENA-003',  '2026-04-14 09:00:00', 1, '2026-04-14 08:30:00'),
(4, 11, 'OTP-ANDREA-004', '2026-04-16 09:00:00', 0, '2026-04-16 08:30:00'),
(5, 12, 'OTP-MIGUEL-005', '2026-04-16 10:00:00', 0, '2026-04-16 09:30:00');

-- ------------------------------------------------------------
-- REGISTRO_SESIONES
-- ------------------------------------------------------------
INSERT INTO registro_sesiones
(id_sesion, id_usuario, token, fecha_inicio, fecha_fin, duracion_minutos, ip, agente_usuario, estado)
VALUES
(1, 1,  'jwt-superadmin-001', '2026-04-15 08:50:00', NULL,                  NULL, '190.12.20.1',  'Chrome/Windows',   'VIGENTE'),
(2, 2,  'jwt-admin-001',      '2026-04-15 08:15:00', '2026-04-15 09:00:00', 45,   '190.12.20.2',  'Chrome/Mac',       'FINALIZADA'),
(3, 4,  'jwt-socio-001',      '2026-04-15 07:55:00', NULL,                  NULL, '190.12.20.3',  'Edge/Windows',     'VIGENTE'),
(4, 5,  'jwt-socio-002',      '2026-04-14 20:10:00', '2026-04-14 21:05:00', 55,   '190.12.20.4',  'Chrome/Linux',     'FINALIZADA'),
(5, 7,  'jwt-view-001',       '2026-04-15 09:11:00', NULL,                  NULL, '190.12.20.5',  'Firefox/Windows',  'VIGENTE'),
(6, 8,  'jwt-view-002',       '2026-04-13 16:15:00', '2026-04-13 17:00:00', 45,   '190.12.20.6',  'Safari/iPad',      'EXPIRADA'),
(7, 11, 'jwt-socio-003',      '2026-04-16 08:10:00', NULL,                  NULL, '190.12.20.7',  'Chrome/Windows',   'VIGENTE'),
(8, 12, 'jwt-socio-004',      '2026-04-16 09:10:00', NULL,                  NULL, '190.12.20.8',  'Chrome/Linux',     'VIGENTE'),
(9, 13, 'jwt-view-003',       '2026-04-16 10:10:00', NULL,                  NULL, '190.12.20.9',  'Firefox/Linux',    'VIGENTE'),
(10,14, 'jwt-view-004',       '2026-04-16 10:40:00', NULL,                  NULL, '190.12.20.10', 'Edge/Windows',     'VIGENTE');

-- ------------------------------------------------------------
-- DOMINIO_AUTORIZADO
-- ------------------------------------------------------------
INSERT INTO dominio_autorizado
(id_dominio, nombre_dominio, motivo_autorizacion, estado, id_usuario_creador, fecha_registro)
VALUES
(1, '@reciclame.pe', 'Dominio interno institucional', 1, 1, '2026-03-25 08:10:00'),
(2, '@alicorp.com',  'Empresa aliada para pilotos',   1, 1, '2026-03-26 09:00:00'),
(3, '@cicla.pe',     'Empresa operadora asociada',    1, 1, '2026-03-27 09:00:00'),
(4, '@upn.edu.pe',   'Usuarios académicos visualizadores', 1, 1, '2026-03-28 09:00:00'),
(5, '@pucp.edu.pe',  'Usuarios académicos visualizadores', 1, 1, '2026-03-28 09:10:00'),
(6, '@viewer.pe',    'Dominio desautorizado',         0, 1, '2026-03-29 09:00:00'),
(7, '@greenloop.pe', 'Consultora aliada',             1, 1, '2026-03-29 09:20:00'),
(8, '@ecoperu.pe',   'Empresa postulante',            1, 1, '2026-04-01 11:00:00');

-- ------------------------------------------------------------
-- POLITICA_CONTRASENA
-- ------------------------------------------------------------
INSERT INTO politica_contrasena
(id_politica, longitud_minima, requiere_mayuscula, requiere_numero, requiere_simbolo, mandato_mfa, expiracion_dias, actualizado_por, actualizado_en)
VALUES
(1, 10, 1, 1, 1, 0, 90, 1, '2026-04-01 08:00:00');

-- ------------------------------------------------------------
-- ESTUDIOS
-- ------------------------------------------------------------
INSERT INTO estudios
(id_estudio, titulo, descripcion, anio, formato, estado, tipo_acceso, archivo_nombre, archivo_url, archivo_tamanio_kb,
 fecha_publicacion, indice_relevancia, informacion_legal, id_usuario_creador, id_usuario_editor, fecha_creacion, fecha_actualizacion, eliminado_en)
VALUES
(1, 'Mapa de valorización de residuos plásticos en Lima Metropolitana',
 'Estudio orientado a identificar flujos, actores y oportunidades de valorización de residuos plásticos.', 2024, 'PDF', 'VIGENTE', 'DESCARGA',
 'mapa_valorizacion_plasticos_2024.pdf', 'https://cdn.reciclame.pe/estudios/mapa_valorizacion_plasticos_2024.pdf', 5420,
 '2024-10-05', 92, 'Documento de uso informativo para análisis sectorial.', 2, 3, '2026-03-29 10:00:00', '2026-04-10 11:00:00', NULL),

(2, 'Diagnóstico de trazabilidad para cadenas de reciclaje',
 'Análisis de brechas de trazabilidad documental y operativa en gestores y recicladores.', 2025, 'PPTX', 'VIGENTE', 'LECTURA',
 'diagnostico_trazabilidad_2025.pptx', 'https://cdn.reciclame.pe/estudios/diagnostico_trazabilidad_2025.pptx', 3180,
 '2025-02-20', 88, 'Versión de consulta para socios y visualizadores.', 3, 3, '2026-03-30 09:30:00', '2026-04-11 14:20:00', NULL),

(3, 'Estudio de mercado para subproductos orgánicos',
 'Revisión de oportunidades de mercado para compost, biofertilizantes y enmiendas.', 2023, 'PDF', 'BORRADOR', 'LECTURA',
 'mercado_subproductos_organicos_2023.pdf', 'https://cdn.reciclame.pe/estudios/mercado_subproductos_organicos_2023.pdf', 2675,
 NULL, 75, 'Borrador interno en revisión legal.', 2, NULL, '2026-04-01 10:15:00', '2026-04-01 10:15:00', NULL),

(4, 'Brechas regulatorias para economía circular en envases',
 'Estudio comparado entre normativa local e instrumentos internacionales aplicables a envases.', 2025, 'PDF', 'VIGENTE', 'DESCARGA',
 'brechas_regulatorias_envases_2025.pdf', 'https://cdn.reciclame.pe/estudios/brechas_regulatorias_envases_2025.pdf', 4890,
 '2025-03-18', 95, 'Análisis con fines de incidencia y apoyo regulatorio.', 2, 2, '2026-04-02 08:40:00', '2026-04-12 09:00:00', NULL),

(5, 'Hoja técnica de indicadores de circularidad empresarial',
 'Documento metodológico para medir desempeño circular en empresas asociadas.', 2022, 'PPTX', 'DEROGADO', 'LECTURA',
 'indicadores_circularidad_2022.pptx', 'https://cdn.reciclame.pe/estudios/indicadores_circularidad_2022.pptx', 1990,
 '2022-11-22', 60, 'Sustituido por una versión más reciente.', 3, 2, '2026-04-03 15:10:00', '2026-04-14 10:10:00', NULL),

(6, 'Potencial de valorización de vidrio posconsumo',
 'Análisis técnico y económico para incrementar el reaprovechamiento de vidrio posconsumo.', 2024, 'PDF', 'VIGENTE', 'DESCARGA',
 'valorizacion_vidrio_2024.pdf', 'https://cdn.reciclame.pe/estudios/valorizacion_vidrio_2024.pdf', 4210,
 '2024-09-10', 84, 'Uso para proyectos y diagnósticos sectoriales.', 2, 3, '2026-04-05 10:00:00', '2026-04-14 13:00:00', NULL),

(7, 'Diagnóstico de capacidades para logística inversa',
 'Documento de análisis sobre operadores, cobertura y costos logísticos en esquemas de recuperación.', 2025, 'PDF', 'VIGENTE', 'LECTURA',
 'logistica_inversa_2025.pdf', 'https://cdn.reciclame.pe/estudios/logistica_inversa_2025.pdf', 3550,
 '2025-01-15', 81, 'Documento de consulta general.', 3, 2, '2026-04-06 09:00:00', '2026-04-15 08:00:00', NULL),

(8, 'Tendencias de innovación en reciclaje químico',
 'Revisión internacional de tecnologías emergentes aplicadas al reciclaje químico.', 2026, 'PPTX', 'BORRADOR', 'LECTURA',
 'innovacion_reciclaje_quimico_2026.pptx', 'https://cdn.reciclame.pe/estudios/innovacion_reciclaje_quimico_2026.pptx', 2800,
 NULL, 77, 'Borrador para validación técnica.', 2, NULL, '2026-04-07 11:00:00', '2026-04-15 09:00:00', NULL),

(9, 'Benchmark regional de indicadores circulares',
 'Comparativa de métricas regionales para seguimiento de estrategias de circularidad.', 2024, 'PDF', 'VIGENTE', 'DESCARGA',
 'benchmark_indicadores_2024.pdf', 'https://cdn.reciclame.pe/estudios/benchmark_indicadores_2024.pdf', 5100,
 '2024-12-02', 90, 'Documento de apoyo para política pública.', 3, 3, '2026-04-08 10:00:00', '2026-04-15 10:30:00', NULL),

(10, 'Guía rápida de trazabilidad documental',
 'Documento práctico para estandarizar evidencias y trazabilidad dentro del repositorio.', 2026, 'PDF', 'VIGENTE', 'LECTURA',
 'guia_trazabilidad_documental_2026.pdf', 'https://cdn.reciclame.pe/estudios/guia_trazabilidad_documental_2026.pdf', 1700,
 '2026-04-01', 70, 'Consulta general para usuarios internos.', 2, 2, '2026-04-08 11:00:00', '2026-04-15 11:00:00', NULL);

-- ------------------------------------------------------------
-- ESTUDIO_CATEGORIA
-- ------------------------------------------------------------
INSERT INTO estudio_categoria
(id_estudio_categoria, id_estudio, id_categoria)
VALUES
(1, 1, 6), (2, 1, 7),
(3, 2, 8), (4, 2, 9),
(5, 3, 6), (6, 3, 10),
(7, 4, 7), (8, 4, 10),
(9, 5, 8),
(10,6, 6), (11,6, 7),
(12,7, 8), (13,7, 10),
(14,8, 9),
(15,9, 7), (16,9, 10),
(17,10,8);

-- ------------------------------------------------------------
-- NORMATIVAS
-- clasificacion usa ahora los valores del ENUM
-- ------------------------------------------------------------
INSERT INTO normativas
(id_normativa, titulo, descripcion, codigo, organismo_emisor, anio, tipo_norma, estado, acceso, alcance,
 campo_aplicacion, clasificacion, obligatoriedad, archivo_nombre, archivo_url, enlace_externo,
 id_usuario_creador, id_usuario_editor, fecha_creacion, fecha_actualizacion, eliminado_en)
VALUES
(1, 'Ley marco para la gestión integral de residuos aprovechables',
 'Marco legal orientado a la gestión integral y valorización de residuos reaprovechables.',
 'LEY-21368', 'Ministerio del Ambiente', 2024, 'LEY_NACIONAL', 'VIGENTE', 'GRATIS', 'NACIONAL',
 'Gestión municipal y empresarial de residuos', 'PRIORIDAD_ALTA', 'Legal Vinculante',
 'ley_marco_residuos_2024.pdf', 'https://cdn.reciclame.pe/normativas/ley_marco_residuos_2024.pdf', NULL,
 2, 3, '2026-03-29 12:00:00', '2026-04-10 09:00:00', NULL),

(2, 'Reglamento técnico sobre responsabilidad extendida del productor',
 'Reglamento técnico aplicable a cadenas REP en envases y embalajes.',
 'REG-REP-009', 'Ministerio de Producción', 2025, 'REGLAMENTO', 'PUBLICADA', 'GRATIS', 'NACIONAL',
 'Fabricantes e importadores de envases', 'PRIORIDAD_MEDIA', 'Legal Vinculante',
 NULL, NULL, 'https://www.gob.pe/produccion/reglamento-rep-009',
 3, 3, '2026-03-30 10:00:00', '2026-04-12 11:00:00', NULL),

(3, 'Directiva internacional sobre circularidad en empaques',
 'Documento internacional de referencia disponible mediante portal especializado.',
 'INT-CIRC-2023', 'Global Circular Forum', 2023, 'OTRO', 'VIGENTE', 'PAGO', 'INTERNACIONAL',
 'Diseño de empaques y reciclabilidad', 'REFERENCIA_TECNICA', 'No Vinculante',
 NULL, NULL, 'https://shop.globalcircularforum.org/directiva-empaques-2023',
 2, NULL, '2026-04-01 09:00:00', '2026-04-01 09:00:00', NULL),

(4, 'Hoja de ruta para infraestructura de valorización regional',
 'Documento programático para orientar infraestructura y logística de valorización.',
 'HDR-VAL-2026', 'Gobierno Regional de Lima', 2026, 'HOJA_DE_RUTA', 'CONSULTA_PUBLICA', 'GRATIS', 'NACIONAL',
 'Infraestructura regional y planificación', 'PLANEAMIENTO', 'No Vinculante',
 NULL, NULL, 'https://consulta.grl.pe/hoja-ruta-valorizacion-2026',
 3, 2, '2026-04-02 13:15:00', '2026-04-12 16:00:00', NULL),

(5, 'Anteproyecto de decreto sobre trazabilidad digital de residuos',
 'Anteproyecto para discutir lineamientos de interoperabilidad y trazabilidad.',
 'ANT-TRAZ-01', 'Ministerio del Ambiente', 2026, 'ANTEPROYECTO', 'BORRADOR_EN_PROCESO', 'GRATIS', 'NACIONAL',
 'Plataformas digitales y reportabilidad', 'INNOVACION_NORMATIVA', 'No Vinculante',
 'anteproyecto_trazabilidad_2026.pdf', 'https://cdn.reciclame.pe/normativas/anteproyecto_trazabilidad_2026.pdf', NULL,
 2, 2, '2026-04-04 10:20:00', '2026-04-14 09:40:00', NULL),

(6, 'Decreto sobre eficiencia energética industrial',
 'Norma para optimización del consumo energético en plantas industriales.',
 'DS-EE-2025', 'Ministerio de Energía y Minas', 2025, 'DECRETO_SUPREMO', 'VIGENTE', 'GRATIS', 'NACIONAL',
 'Industria manufacturera', 'OBLIGATORIA', 'Legal Vinculante',
 'decreto_eficiencia_energetica_2025.pdf', 'https://cdn.reciclame.pe/normativas/decreto_eficiencia_energetica_2025.pdf', NULL,
 2, 2, '2026-04-05 10:00:00', '2026-04-15 10:00:00', NULL),

(7, 'Guía internacional de simbiosis industrial',
 'Guía internacional para proyectos de simbiosis industrial y reutilización de subproductos.',
 'GUIA-SI-2024', 'UN Circular Program', 2024, 'OTRO', 'VIGENTE', 'PAGO', 'INTERNACIONAL',
 'Parques industriales', 'REFERENCIA_TECNICA', 'No Vinculante',
 NULL, NULL, 'https://store.uncircular.org/guia-simbiosis-2024',
 3, NULL, '2026-04-05 11:00:00', '2026-04-15 11:00:00', NULL),

(8, 'Reglamento para valorización de RAEE',
 'Reglamento nacional para la gestión y valorización de residuos de aparatos eléctricos y electrónicos.',
 'RAEE-2025', 'Ministerio del Ambiente', 2025, 'REGLAMENTO', 'VIGENTE', 'GRATIS', 'NACIONAL',
 'RAEE y logística inversa', 'PRIORIDAD_ALTA', 'Legal Vinculante',
 NULL, NULL, 'https://www.gob.pe/minam/raee-2025',
 2, 3, '2026-04-06 08:00:00', '2026-04-15 12:00:00', NULL),

(9, 'Ley comparada sobre envases reutilizables',
 'Compendio internacional de regulaciones sobre envases reutilizables.',
 'CMP-ENV-2024', 'European Circular Board', 2024, 'OTRO', 'PUBLICADA', 'PAGO', 'INTERNACIONAL',
 'Envases y embalajes', 'COMPLEMENTARIA', 'No Vinculante',
 NULL, NULL, 'https://shop.ecb.eu/envases-reutilizables-2024',
 2, NULL, '2026-04-06 09:00:00', '2026-04-15 13:00:00', NULL),

(10, 'Lineamientos para compras públicas circulares',
 'Documento para integrar criterios de circularidad en procesos de contratación pública.',
 'LCP-2026', 'OSCE', 2026, 'OTRO', 'CONSULTA_PUBLICA', 'GRATIS', 'NACIONAL',
 'Contratación pública sostenible', 'PLANEAMIENTO', 'No Vinculante',
 'lineamientos_compras_circulares_2026.pdf', 'https://cdn.reciclame.pe/normativas/lineamientos_compras_circulares_2026.pdf', NULL,
 3, 2, '2026-04-07 10:00:00', '2026-04-15 14:00:00', NULL);

-- ------------------------------------------------------------
-- NORMATIVA_CATEGORIA
-- ------------------------------------------------------------
INSERT INTO normativa_categoria
(id_normativa_categoria, id_normativa, id_categoria)
VALUES
(1, 1, 2),  (2, 1, 1),
(3, 2, 4),  (4, 2, 1),
(5, 3, 1),  (6, 3, 5),
(7, 4, 2),
(8, 5, 1),  (9, 5, 5),
(10,6, 5),
(11,7, 5),
(12,8, 2),  (13,8, 4),
(14,9, 1),  (15,9, 5),
(16,10,1),  (17,10,2);

-- ------------------------------------------------------------
-- REGISTRO_DESCARGA
-- ------------------------------------------------------------
INSERT INTO registro_descarga
(id_descarga, id_usuario, tipo_documento, id_documento, nombre_documento, url_descargada, fecha_descarga)
VALUES
(1, 4,  'ESTUDIO',   1, 'Mapa de valorización de residuos plásticos en Lima Metropolitana',
 'https://cdn.reciclame.pe/estudios/mapa_valorizacion_plasticos_2024.pdf', '2026-04-15 08:05:00'),
(2, 5,  'ESTUDIO',   4, 'Brechas regulatorias para economía circular en envases',
 'https://cdn.reciclame.pe/estudios/brechas_regulatorias_envases_2025.pdf', '2026-04-14 20:20:00'),
(3, 4,  'NORMATIVA', 1, 'Ley marco para la gestión integral de residuos aprovechables',
 'https://cdn.reciclame.pe/normativas/ley_marco_residuos_2024.pdf', '2026-04-15 08:08:00'),
(4, 5,  'NORMATIVA', 5, 'Anteproyecto de decreto sobre trazabilidad digital de residuos',
 'https://cdn.reciclame.pe/normativas/anteproyecto_trazabilidad_2026.pdf', '2026-04-12 19:45:00'),
(5, 11, 'ESTUDIO',   6, 'Potencial de valorización de vidrio posconsumo',
 'https://cdn.reciclame.pe/estudios/valorizacion_vidrio_2024.pdf', '2026-04-16 08:30:00'),
(6, 12, 'ESTUDIO',   9, 'Benchmark regional de indicadores circulares',
 'https://cdn.reciclame.pe/estudios/benchmark_indicadores_2024.pdf', '2026-04-16 09:45:00'),
(7, 15, 'NORMATIVA', 6, 'Decreto sobre eficiencia energética industrial',
 'https://cdn.reciclame.pe/normativas/decreto_eficiencia_energetica_2025.pdf', '2026-04-16 11:20:00');

-- ------------------------------------------------------------
-- NOTIFICACIONES
-- ------------------------------------------------------------
INSERT INTO notificaciones
(id_notificacion, id_usuario, titulo, mensaje, tipo, leido, enlace_referencia, tipo_entidad, id_entidad, fecha)
VALUES
(1, 2,  'Nueva solicitud de registro',       'Se recibió una nueva solicitud pendiente de revisión.',                 'SOLICITUD', 0, '/usuarios/solicitudes/5',  'SOLICITUD_REGISTRO', 5,  '2026-04-05 12:01:00'),
(2, 4,  'Nuevo estudio disponible',          'Se publicó un nuevo estudio con acceso de descarga para socios.',       'ESTUDIO',   1, '/estudios/4',             'ESTUDIO',             4,  '2026-04-12 09:05:00'),
(3, 5,  'Normativa actualizada',             'Se actualizó el estado de una normativa relevante para tu sector.',     'NORMATIVA', 0, '/normativas/2',           'NORMATIVA',           2,  '2026-04-12 11:05:00'),
(4, 7,  'Bienvenido al repositorio',         'Tu cuenta ha sido aprobada y ya puedes ingresar al sistema.',           'SISTEMA',   1, '/home',                   'USUARIO',             7,  '2026-04-01 08:31:00'),
(5, 8,  'Nueva publicación institucional',   'Se publicó un nuevo boletín editorial en la sección publicaciones.',   'SISTEMA',   0, '/publicaciones/2',        'PUBLICACION',         2,  '2026-04-14 08:00:00'),
(6, 1,  'Actividad administrativa relevante','Se registró el bloqueo manual de un usuario socio.',                    'SISTEMA',   0, '/auditoria/acciones/2',   'AUDITORIA',           2,  '2026-04-10 16:15:00'),
(7, 11, 'Nueva normativa disponible',        'Ya puedes revisar la nueva normativa sobre eficiencia energética.',     'NORMATIVA', 0, '/normativas/6',           'NORMATIVA',           6,  '2026-04-15 10:05:00'),
(8, 12, 'Actualización de normativa RAEE',   'Se actualizó el reglamento RAEE con un nuevo enlace de consulta.',      'NORMATIVA', 0, '/normativas/8',           'NORMATIVA',           8,  '2026-04-15 12:05:00'),
(9, 13, 'Nueva guía publicada',              'Hay una nueva guía internacional de simbiosis industrial.',             'NORMATIVA', 0, '/normativas/7',           'NORMATIVA',           7,  '2026-04-15 11:05:00'),
(10,14, 'Tu acceso sigue activo',            'Tu cuenta visualizadora continúa habilitada sin incidencias.',          'SISTEMA',   1, '/perfil',                 'USUARIO',             14, '2026-04-15 15:00:00');

-- ------------------------------------------------------------
-- INDICADOR
-- ------------------------------------------------------------
INSERT INTO indicador
(id_indicador, nombre, descripcion, categoria, unidad, estado)
VALUES
(1, 'Toneladas valorizadas',              'Toneladas de residuos valorizados por periodo',           'Valorización', 'toneladas', 1),
(2, 'Tasa de reciclaje empresarial',      'Porcentaje de residuos reciclados en empresas asociadas', 'Reciclaje',    '%',         1),
(3, 'Empresas reportando trazabilidad',   'Número de empresas que reportan trazabilidad digital',    'Trazabilidad', 'empresas',  1),
(4, 'Aprovechamiento de orgánicos',       'Volumen de orgánicos aprovechados mediante compostaje',   'Orgánicos',    'toneladas', 1),
(5, 'Cobertura de logística inversa',     'Cobertura geográfica de operadores de logística inversa', 'Logística',    '%',         1);

-- ------------------------------------------------------------
-- INDICADOR_VALOR
-- ------------------------------------------------------------
INSERT INTO indicador_valor
(id_valor, id_indicador, valor, anio, fecha_registro)
VALUES
(1,  1, 1250.5000, 2022, '2022-12-31 18:00:00'),
(2,  1, 1489.2000, 2023, '2023-12-31 18:00:00'),
(3,  1, 1768.9000, 2024, '2024-12-31 18:00:00'),
(4,  2, 32.4000,   2022, '2022-12-31 18:00:00'),
(5,  2, 38.7000,   2023, '2023-12-31 18:00:00'),
(6,  2, 44.9000,   2024, '2024-12-31 18:00:00'),
(7,  3, 15.0000,   2023, '2023-12-31 18:00:00'),
(8,  3, 24.0000,   2024, '2024-12-31 18:00:00'),
(9,  4, 310.2500,  2023, '2023-12-31 18:00:00'),
(10, 4, 402.8000,  2024, '2024-12-31 18:00:00'),
(11, 5, 18.5000,   2023, '2023-12-31 18:00:00'),
(12, 5, 27.2000,   2024, '2024-12-31 18:00:00');

-- ------------------------------------------------------------
-- PUBLICACION
-- ------------------------------------------------------------
INSERT INTO publicacion
(id_publicacion, titulo, contenido, tipo, archivo_nombre, archivo_url, fecha_publicacion, id_usuario_creador, creado_en, eliminado_en)
VALUES
(1, 'Boletín mensual de circularidad - Marzo 2026',
 'Resumen de hitos normativos, estudios publicados y avances de valorización empresarial.',
 'NEWSLETTER', 'boletin_marzo_2026.pdf', 'https://cdn.reciclame.pe/publicaciones/boletin_marzo_2026.pdf',
 '2026-03-31', 2, '2026-03-31 18:00:00', NULL),

(2, 'Editorial: trazabilidad como base de la confianza',
 'Reflexión institucional sobre el valor de la trazabilidad para la toma de decisiones.',
 'EDITORIAL', NULL, NULL,
 '2026-04-14', 1, '2026-04-14 07:40:00', NULL),

(3, 'Boletín mensual de circularidad - Abril 2026',
 'Compendio de publicaciones, normativas y principales cambios del mes.',
 'NEWSLETTER', 'boletin_abril_2026.pdf', 'https://cdn.reciclame.pe/publicaciones/boletin_abril_2026.pdf',
 '2026-04-30', 2, '2026-04-30 18:00:00', NULL);

-- ------------------------------------------------------------
-- ACTIVIDAD_SISTEMA
-- ------------------------------------------------------------
INSERT INTO actividad_sistema
(id_actividad, tipo, id_referencia, descripcion, accion, fecha)
VALUES
(1, 'ESTUDIO',     4, 'Se añadió nuevo estudio: Brechas regulatorias para economía circular en envases', 'AÑADIDO',    '2026-04-12 09:00:00'),
(2, 'NORMATIVA',   2, 'Se modificó normativa: Reglamento técnico sobre responsabilidad extendida del productor', 'MODIFICADO', '2026-04-12 11:00:00'),
(3, 'USUARIO',     6, 'Se bloqueó la cuenta del usuario Roberto Bravo', 'MODIFICADO', '2026-04-10 16:10:00'),
(4, 'PUBLICACION', 2, 'Se añadió nueva publicación: Editorial: trazabilidad como base de la confianza', 'AÑADIDO', '2026-04-14 07:40:00'),
(5, 'INDICADOR',   1, 'Se actualizaron valores históricos del indicador Toneladas valorizadas', 'MODIFICADO', '2026-04-13 09:20:00'),
(6, 'NORMATIVA',   6, 'Se añadió nueva normativa: Decreto sobre eficiencia energética industrial', 'AÑADIDO', '2026-04-15 10:00:00'),
(7, 'ESTUDIO',     9, 'Se añadió nuevo estudio: Benchmark regional de indicadores circulares', 'AÑADIDO', '2026-04-15 10:30:00');

-- ------------------------------------------------------------
-- AUDITORIA_ACCIONES_ADMIN
-- ------------------------------------------------------------
INSERT INTO auditoria_acciones_admin
(id_auditoria, id_usuario, accion, modulo, id_entidad, descripcion, datos_anteriores, datos_nuevos, fecha, ip)
VALUES
(1, 2, 'APROBAR',   'usuarios',      7, 'Se aprobó la solicitud de Elena Torres y se creó la cuenta.', NULL,
 JSON_OBJECT('id_usuario', 7, 'correo', 'elena.torres@upn.edu.pe', 'rol', 'VISUALIZADOR'),
 '2026-04-01 08:30:00', '190.12.30.2'),

(2, 3, 'BLOQUEAR',  'usuarios',      6, 'Se bloqueó la cuenta de Roberto Bravo por revisión administrativa.',
 JSON_OBJECT('estado_cuenta', 'ACTIVO'),
 JSON_OBJECT('estado_cuenta', 'BLOQUEADO'),
 '2026-04-10 16:10:00', '190.12.30.3'),

(3, 2, 'CREAR',     'estudios',      4, 'Se creó el estudio Brechas regulatorias para economía circular en envases.',
 NULL,
 JSON_OBJECT('titulo', 'Brechas regulatorias para economía circular en envases', 'estado', 'VIGENTE'),
 '2026-04-02 08:40:00', '190.12.30.2'),

(4, 3, 'EDITAR',    'normativas',    2, 'Se actualizó información de acceso y clasificación de normativa REP.',
 JSON_OBJECT('clasificacion', 'COMPLEMENTARIA'),
 JSON_OBJECT('clasificacion', 'PRIORIDAD_MEDIA'),
 '2026-04-12 11:00:00', '190.12.30.3'),

(5, 1, 'CONFIGURAR','configuracion', 1, 'Se actualizó la política de contraseñas del sistema.',
 JSON_OBJECT('longitud_minima', 8),
 JSON_OBJECT('longitud_minima', 10),
 '2026-04-01 08:00:00', '190.12.30.1');

-- ------------------------------------------------------------
-- HISTORIAL_ESTUDIOS
-- ------------------------------------------------------------
INSERT INTO historial_estudios
(id_historial, id_estudio, campo_modificado, valor_anterior, valor_nuevo, modificado_por, es_rollback, id_version_origen, fecha_cambio)
VALUES
(1, 4, 'estado',            'BORRADOR', 'VIGENTE', 2, 0, NULL, '2026-04-02 08:50:00'),
(2, 4, 'indice_relevancia', '90',       '95',      2, 0, NULL, '2026-04-12 09:00:00'),
(3, 2, 'archivo_url',       'https://cdn.reciclame.pe/estudios/diagnostico_trazabilidad_v1.pptx',
                             'https://cdn.reciclame.pe/estudios/diagnostico_trazabilidad_2025.pptx', 3, 0, NULL, '2026-04-11 14:20:00'),
(4, 9, 'estado',            'BORRADOR', 'VIGENTE', 3, 0, NULL, '2026-04-15 10:25:00');

-- ------------------------------------------------------------
-- HISTORIAL_NORMATIVAS
-- ------------------------------------------------------------
INSERT INTO historial_normativas
(id_historial, id_normativa, campo_modificado, valor_anterior, valor_nuevo, modificado_por, es_rollback, id_version_origen, fecha_cambio)
VALUES
(1, 2, 'clasificacion', 'COMPLEMENTARIA',      'PRIORIDAD_MEDIA',      3, 0, NULL, '2026-04-12 11:00:00'),
(2, 4, 'estado',        'BORRADOR_EN_PROCESO', 'CONSULTA_PUBLICA',     2, 0, NULL, '2026-04-12 16:00:00'),
(3, 5, 'archivo_url',   NULL,                  'https://cdn.reciclame.pe/normativas/anteproyecto_trazabilidad_2026.pdf', 2, 0, NULL, '2026-04-14 09:40:00'),
(4, 6, 'estado',        'PUBLICADA',           'VIGENTE',              2, 0, NULL, '2026-04-15 10:00:00');

-- ------------------------------------------------------------
-- HISTORIAL_ROLES
-- id_rol_anterior / id_rol_nuevo referencian a rol.id_rol
-- ------------------------------------------------------------
INSERT INTO historial_roles
(id_historial_rol, id_usuario_afectado, id_rol_anterior, id_rol_nuevo, estado_anterior, estado_nuevo, autorizado_por, motivo, fecha_cambio)
VALUES
(1, 7,  NULL, 4, NULL,      'ACTIVO',    2, 'Aprobación de solicitud de registro.', '2026-04-01 08:30:00'),
(2, 6,  3,    3, 'ACTIVO',  'BLOQUEADO', 3, 'Bloqueo por revisión administrativa.', '2026-04-10 16:10:00'),
(3, 11, NULL, 3, NULL,      'ACTIVO',    2, 'Alta de nuevo socio aprobado.',        '2026-04-06 10:00:00'),
(4, 12, NULL, 3, NULL,      'ACTIVO',    3, 'Alta de nuevo socio aprobado.',        '2026-04-06 11:00:00'),
(5, 13, NULL, 4, NULL,      'ACTIVO',    2, 'Alta de nuevo visualizador.',          '2026-04-07 12:00:00'),
(6, 14, NULL, 4, NULL,      'ACTIVO',    2, 'Alta de nuevo visualizador.',          '2026-04-07 13:00:00'),
(7, 15, NULL, 3, NULL,      'ACTIVO',    3, 'Alta de nuevo socio.',                 '2026-04-08 10:00:00'),
(8, 5,  3,    4, 'ACTIVO',  'ACTIVO',    1, 'Cambio de socio a visualizador.',      '2026-04-16 09:00:00');

-- ------------------------------------------------------------
-- CONFIGURACION_CHATBOT
-- ------------------------------------------------------------
INSERT INTO configuracion_chatbot
(id_chatbot, activo, proveedor, tasa_resolucion_pct, tiempo_respuesta_seg, consultas_hoy, actualizado_por, actualizado_en)
VALUES
(1, 1, 'OpenAI', 87.50, 2.80, 34, 1, '2026-04-15 07:30:00');

-- ------------------------------------------------------------
-- CHAT_SESION
-- ------------------------------------------------------------
INSERT INTO chat_sesion
(id_chat, id_usuario, fecha_inicio, fecha_fin, activa)
VALUES
(1, 4,  '2026-04-15 08:06:00', '2026-04-15 08:10:00', 0),
(2, 7,  '2026-04-15 09:12:00', NULL,                  1),
(3, 5,  '2026-04-14 20:25:00', '2026-04-14 20:32:00', 0),
(4, 11, '2026-04-16 08:35:00', '2026-04-16 08:40:00', 0),
(5, 12, '2026-04-16 09:50:00', NULL,                  1);

-- ------------------------------------------------------------
-- CHAT_MENSAJE
-- ------------------------------------------------------------
INSERT INTO chat_mensaje
(id_mensaje, id_chat, emisor, mensaje, fecha)
VALUES
(1, 1, 'USUARIO', '¿Qué estudios puedo descargar como socio?', '2026-04-15 08:06:10'),
(2, 1, 'SISTEMA', 'Puedes descargar los estudios con tipo de acceso DESCARGA si tu rol es SOCIO.', '2026-04-15 08:06:12'),
(3, 1, 'USUARIO', 'Muéstrame los más recientes.', '2026-04-15 08:06:30'),
(4, 1, 'SISTEMA', 'Los más recientes son Brechas regulatorias para economía circular en envases y Diagnóstico de trazabilidad para cadenas de reciclaje.', '2026-04-15 08:06:33'),
(5, 2, 'USUARIO', '¿Dónde veo las normativas nacionales?', '2026-04-15 09:12:05'),
(6, 2, 'SISTEMA', 'En el módulo Repositorio Normativo puedes filtrar por alcance NACIONAL.', '2026-04-15 09:12:07'),
(7, 3, 'USUARIO', '¿Qué significa una normativa en consulta pública?', '2026-04-14 20:25:20'),
(8, 3, 'SISTEMA', 'Significa que aún no está definitiva y puede recibir observaciones antes de su publicación final.', '2026-04-14 20:25:24'),
(9, 4, 'USUARIO', '¿Existe alguna normativa nueva sobre eficiencia energética?', '2026-04-16 08:35:10'),
(10,4, 'SISTEMA', 'Sí. Se añadió el Decreto sobre eficiencia energética industrial en el módulo de normativas.', '2026-04-16 08:35:12'),
(11,5, 'USUARIO', 'Muéstrame normas sobre RAEE.', '2026-04-16 09:50:05'),
(12,5, 'SISTEMA', 'Puedes revisar el Reglamento para valorización de RAEE filtrando por gestión de residuos y REP.', '2026-04-16 09:50:08');

COMMIT;

-- ============================================================
-- RESUMEN FINAL
-- ============================================================
-- Total tablas: 27
--
-- Seguridad/Usuarios:
-- rol, usuarios, usuario_empresa, solicitud_registro,
-- intento_login, recuperacion_contrasena, registro_sesiones
--
-- Contenido:
-- categoria, estudios, estudio_categoria,
-- normativas, normativa_categoria
--
-- Trazabilidad:
-- registro_descarga
--
-- Notificaciones:
-- notificaciones, actividad_sistema
--
-- Adicionales:
-- indicador, indicador_valor, publicacion
--
-- Auditoría:
-- auditoria_acciones_admin, historial_estudios,
-- historial_normativas, historial_roles
--
-- Configuración:
-- dominio_autorizado, politica_contrasena,
-- configuracion_chatbot
--
-- Chatbot:
-- chat_sesion, chat_mensaje
-- ============================================================