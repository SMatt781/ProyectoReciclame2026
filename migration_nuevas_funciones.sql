-- ============================================================
-- MIGRACIÓN: Nuevas funciones Mi Espacio (Socio) y Mis Lecturas + Citas (Visualizador)
-- Aplicar sobre la BD existente sin modificar tablas actuales
-- ============================================================

USE reciclame;

-- ============================================================
-- TABLA: contenido_guardado
-- Permite al Socio guardar estudios y normativas en "Mi Espacio"
-- ============================================================
CREATE TABLE IF NOT EXISTS contenido_guardado (
    id_guardado      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_usuario       BIGINT UNSIGNED NOT NULL,
    tipo_documento   ENUM('ESTUDIO','NORMATIVA') NOT NULL,
    id_documento     BIGINT UNSIGNED NOT NULL,
    nombre_documento VARCHAR(400)    NOT NULL,
    coleccion        VARCHAR(100)    NULL COMMENT 'Carpeta o colección opcional',
    fecha_guardado   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id_guardado),
    UNIQUE KEY uq_guardado_usuario_doc (id_usuario, tipo_documento, id_documento),
    INDEX idx_guardado_usuario (id_usuario, fecha_guardado),

    CONSTRAINT fk_guardado_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Documentos guardados por el Socio en Mi Espacio.';

-- ============================================================
-- TABLA: historial_lectura
-- Registra automáticamente los documentos que el Visualizador consulta
-- ============================================================
CREATE TABLE IF NOT EXISTS historial_lectura (
    id_lectura       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_usuario       BIGINT UNSIGNED NOT NULL,
    tipo_documento   ENUM('ESTUDIO','NORMATIVA') NOT NULL,
    id_documento     BIGINT UNSIGNED NOT NULL,
    nombre_documento VARCHAR(400)    NOT NULL,
    veces_visto      INT UNSIGNED    NOT NULL DEFAULT 1,
    primera_lectura  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultima_lectura   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id_lectura),
    UNIQUE KEY uq_lectura_usuario_doc (id_usuario, tipo_documento, id_documento),
    INDEX idx_lectura_usuario_fecha (id_usuario, ultima_lectura),

    CONSTRAINT fk_lectura_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Historial automático de documentos consultados por el Visualizador.';
