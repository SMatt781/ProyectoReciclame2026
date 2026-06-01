-- ============================================================
-- MÓDULO IA - TABLAS DE CACHÉ Y USO
-- Aplicar DESPUÉS de migration_carpetas.sql
-- Base de datos: reciclame
-- ============================================================

USE reciclame;

-- Caché unificado: sirve para ESTUDIO y NORMATIVA
CREATE TABLE IF NOT EXISTS ai_resumen_cache (
    id_cache      BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    tipo_doc      VARCHAR(10)      NOT NULL DEFAULT 'ESTUDIO' COMMENT 'ESTUDIO | NORMATIVA',
    id_documento  BIGINT UNSIGNED  NOT NULL,
    resumen       TEXT             NOT NULL,
    generado_en   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    proveedor     VARCHAR(20)      NOT NULL DEFAULT 'GEMINI',

    PRIMARY KEY (id_cache),
    UNIQUE KEY uq_cache_doc (tipo_doc, id_documento),
    INDEX idx_cache_fecha (generado_en)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Caché de resúmenes ejecutivos generados por IA.';

-- Registro de uso de IA por usuario (rate limiting)
CREATE TABLE IF NOT EXISTS ai_usage_log (
    id_log          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    id_usuario      BIGINT UNSIGNED  NOT NULL,
    tipo_consulta   VARCHAR(30)      NOT NULL COMMENT 'RESUMEN | CHATBOT',
    fecha_consulta  DATE             NOT NULL,
    tokens_usados   INT UNSIGNED     NOT NULL DEFAULT 0,

    PRIMARY KEY (id_log),
    INDEX idx_usage_usuario_fecha (id_usuario, fecha_consulta),

    CONSTRAINT fk_usage_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Registro de uso de IA por usuario para rate limiting diario.';
