-- ============================================================
-- MIGRACIÓN: Carpetas personalizadas en Mi Espacio (Socio)
-- Aplicar DESPUÉS de migration_nuevas_funciones.sql
-- ============================================================

USE reciclame;

-- ============================================================
-- TABLA: espacio_carpeta
-- Carpetas personalizadas del Socio para organizar su contenido
-- ============================================================
CREATE TABLE IF NOT EXISTS espacio_carpeta (
    id_carpeta     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_usuario     BIGINT UNSIGNED NOT NULL,
    nombre         VARCHAR(100)    NOT NULL,
    color          VARCHAR(20)     NOT NULL DEFAULT '#006d37',
    emoji          VARCHAR(10)     NULL,
    fecha_creacion DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id_carpeta),
    UNIQUE KEY uq_carpeta_usuario_nombre (id_usuario, nombre),
    INDEX idx_carpeta_usuario (id_usuario),

    CONSTRAINT fk_carpeta_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Carpetas personalizadas del Socio en Mi Espacio.';

-- ============================================================
-- MODIFICAR: contenido_guardado
-- Reemplaza columna coleccion (VARCHAR) por FK real a carpeta
-- ============================================================
ALTER TABLE contenido_guardado
    DROP COLUMN coleccion,
    ADD COLUMN id_carpeta BIGINT UNSIGNED NULL
        COMMENT 'Carpeta asignada; NULL = Sin clasificar',
    ADD CONSTRAINT fk_guardado_carpeta
        FOREIGN KEY (id_carpeta) REFERENCES espacio_carpeta(id_carpeta)
        ON DELETE SET NULL ON UPDATE CASCADE;
