-- ============================================================================
-- Migración: Hacer apellidos NULL para soportar RUC (empresas sin apellidos)
-- Descripción: Para RUC (empresas), no tiene sentido tener apellidoPaterno y
--              apellidoMaterno obligatorios. Esta migración los hace opcionales.
-- ============================================================================

-- 1. Modificar columnas para permitir NULL
ALTER TABLE usuarios MODIFY COLUMN apellidoPaterno VARCHAR(255) NULL;
ALTER TABLE usuarios MODIFY COLUMN apellidoMaterno VARCHAR(255) NULL;

-- 2. Crear índice (si aún no existe) para búsquedas rápidas
-- El índice compuesto ya debería existir, pero si no:
-- CREATE INDEX idx_nombres_apellidos ON usuarios(apellidoPaterno, apellidoMaterno);

-- 3. Verificar datos existentes (debería estar todo bien)
-- Consulta para ver si hay NULLs:
-- SELECT COUNT(*) FROM usuarios WHERE apellidoPaterno IS NULL OR apellidoMaterno IS NULL;

-- 4. Validar que usuario_empresa tiene razón social
-- Los RUC guardados tendrán la razón social en usuario_empresa.razon_social
-- SELECT ue.razon_social FROM usuario_empresa ue
-- WHERE ue.id_usuario IN (SELECT id_usuario FROM identificacion WHERE tipo = 'RUC');

COMMIT;
