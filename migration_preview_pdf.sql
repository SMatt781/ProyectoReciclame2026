-- ============================================================
-- MIGRACIÓN: Preview PDF pre-generado para estudios PPTX
-- Aplicar DESPUÉS de los scripts base de la BD
-- ============================================================

USE reciclame;

ALTER TABLE estudios
    ADD COLUMN archivo_preview_url VARCHAR(500) NULL
        COMMENT 'Clave S3 del PDF convertido de PPTX con marca de agua (generado en upload)';
