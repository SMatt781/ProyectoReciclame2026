-- Script para permitir NULL en la columna ruc de la tabla usuario_empresa
-- Esto permite editar usuarios sin RUC

-- 1. Primero, eliminar la restricción UNIQUE/NOT NULL actual en ruc (si existe)
ALTER TABLE usuario_empresa DROP INDEX IF EXISTS uq_usuario_empresa_ruc;

-- 2. Modificar la columna ruc para permitir NULL
ALTER TABLE usuario_empresa MODIFY COLUMN ruc VARCHAR(20) NULL;

-- 3. Verificar que los cambios se aplicaron correctamente
SHOW CREATE TABLE usuario_empresa;
