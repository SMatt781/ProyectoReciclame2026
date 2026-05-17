-- Script para permitir NULL en la columna dni de la tabla usuarios
-- Esto permite crear usuarios con solo RUC y sin DNI

-- 1. Primero, eliminar la restricción UNIQUE actual en dni
ALTER TABLE usuarios DROP INDEX IF EXISTS uq_usuarios_dni;

-- 2. Modificar la columna dni para permitir NULL
ALTER TABLE usuarios MODIFY COLUMN dni VARCHAR(20) NULL;

-- 3. Crear una nueva restricción UNIQUE que solo aplique a valores no-NULL
-- Esta restricción permite múltiples filas con NULL en dni
ALTER TABLE usuarios ADD UNIQUE KEY uq_usuarios_dni_not_null (dni) WHERE dni IS NOT NULL;

-- Verificar que los cambios se aplicaron correctamente
SHOW CREATE TABLE usuarios;
