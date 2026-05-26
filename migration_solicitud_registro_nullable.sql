-- ============================================================================
-- Migración: Hacer apellidos NULL en solicitud_registro para RUC
-- Descripción: Para RUC (empresas), los apellidos no son requeridos.
--              Esta migración hace que apellido_paterno y apellido_materno sean opcionales
--              en la tabla solicitud_registro para mantener consistencia con la tabla usuarios.
-- ============================================================================

-- 1. Modificar columnas para permitir NULL
ALTER TABLE solicitud_registro MODIFY COLUMN apellido_paterno VARCHAR(255) NULL;
ALTER TABLE solicitud_registro MODIFY COLUMN apellido_materno VARCHAR(255) NULL;

-- 2. Verificar cambios (opcional, para inspección)
-- DESCRIBE solicitud_registro;

COMMIT;
