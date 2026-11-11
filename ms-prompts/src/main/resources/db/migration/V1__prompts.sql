-- Esquema inicial de ms-prompts (v3). Todo vive en el schema `prompts`.

-- Un prompt de la biblioteca.
CREATE TABLE prompt (
    id                    UUID          PRIMARY KEY,
    titulo                VARCHAR(150)  NOT NULL,
    -- Para qué sirve: la línea que se lee en la tarjeta.
    proposito             VARCHAR(500)  NOT NULL,
    -- El texto del prompt, tal cual se copia.
    contenido             TEXT          NOT NULL,
    categoria             VARCHAR(30)   NOT NULL,
    carrera               VARCHAR(120),
    -- Con qué herramienta de IA se probó (ChatGPT, Claude, Gemini…). Texto libre, como `herramientas`.
    herramienta           VARCHAR(80),
    -- Claim oid de quien lo publicó.
    autor_id              VARCHAR(100)  NOT NULL,
    visibilidad           VARCHAR(20)   NOT NULL,
    -- De qué prompt es copia. Si el original se borra, la copia sigue, solo pierde el enlace.
    origen_id             UUID          REFERENCES prompt (id) ON DELETE SET NULL,
    bifurcaciones         INTEGER       NOT NULL DEFAULT 0,
    -- Valoraciones desnormalizadas: ordenar por promedio no recorre la tabla de valoraciones.
    suma_estrellas        INTEGER       NOT NULL DEFAULT 0,
    cantidad_valoraciones INTEGER       NOT NULL DEFAULT 0,
    -- Moderación: aparte de la visibilidad, para que el autor no lo revierta con un PUT.
    oculto                BOOLEAN       NOT NULL DEFAULT FALSE,
    motivo_ocultamiento   VARCHAR(500),
    ocultado_por          VARCHAR(100),
    ocultado_en           TIMESTAMPTZ,
    creado_en             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    actualizado_en        TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
CREATE INDEX ix_prompt_visible ON prompt (visibilidad, oculto, creado_en DESC);
CREATE INDEX ix_prompt_autor ON prompt (autor_id);

-- Una valoración por persona y prompt: la clave lo garantiza, y valorar de nuevo la reemplaza.
CREATE TABLE valoracion (
    prompt_id    UUID          NOT NULL REFERENCES prompt (id) ON DELETE CASCADE,
    usuario_id   VARCHAR(100)  NOT NULL,
    estrellas    SMALLINT      NOT NULL CHECK (estrellas BETWEEN 1 AND 5),
    valorado_en  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    PRIMARY KEY (prompt_id, usuario_id)
);
