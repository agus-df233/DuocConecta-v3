-- Moderación de perfiles (v3): el ADMINISTRADOR puede sacar un perfil de los listados públicos.
-- Va aparte de `visible` a propósito: esa la elige la persona, y si el administrador la cambiara,
-- la persona podría volver a mostrarse con su propio interruptor.

ALTER TABLE usuario ADD COLUMN oculto_por_moderacion BOOLEAN      NOT NULL DEFAULT FALSE;
ALTER TABLE usuario ADD COLUMN motivo_moderacion     VARCHAR(500);
-- Claim oid del administrador que lo ocultó.
ALTER TABLE usuario ADD COLUMN moderado_por          VARCHAR(100);
ALTER TABLE usuario ADD COLUMN moderado_en           TIMESTAMPTZ;

COMMENT ON COLUMN usuario.oculto_por_moderacion IS 'Oculto por un administrador: no sale en listados ni como autor';
