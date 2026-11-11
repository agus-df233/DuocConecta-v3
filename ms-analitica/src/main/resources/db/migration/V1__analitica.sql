-- Esquema inicial de ms-analitica (v3). Todo vive en el schema `analitica`.
-- Son vistas que arman los consumidores de Kafka: si se pierden, se reconstruyen releyendo los
-- tópicos (por eso la retención de interacciones es de 7 días, igual que la ventana de tendencia).

-- Cuántas interacciones tuvo cada recurso, por día y por tipo. La tendencia suma los últimos 7 días.
CREATE TABLE interaccion_diaria (
    tipo_recurso    VARCHAR(20)   NOT NULL,
    recurso_id      VARCHAR(100)  NOT NULL,
    dia             DATE          NOT NULL,
    tipo            VARCHAR(40)   NOT NULL,
    cantidad        BIGINT        NOT NULL,
    PRIMARY KEY (tipo_recurso, recurso_id, dia, tipo)
);
CREATE INDEX ix_interaccion_dia ON interaccion_diaria (tipo_recurso, dia);

-- Recursos ocultos por moderación o eliminados: no salen en la tendencia aunque tengan interacciones.
CREATE TABLE recurso_excluido (
    recurso_id      VARCHAR(100)  PRIMARY KEY,
    tipo_recurso    VARCHAR(20)   NOT NULL,
    motivo          VARCHAR(20)   NOT NULL,
    desde           TIMESTAMPTZ   NOT NULL
);

-- Lo que hizo cada persona en las últimas 24 h, para contar ráfagas. Se borra pasado ese plazo,
-- igual que la retención del tópico: no se guarda la actividad de nadie más de lo necesario.
CREATE TABLE actividad_reciente (
    id              UUID          PRIMARY KEY,
    usuario_id      VARCHAR(100)  NOT NULL,
    tipo            VARCHAR(40)   NOT NULL,
    recurso_id      VARCHAR(100),
    ocurrido_en     TIMESTAMPTZ   NOT NULL
);
CREATE INDEX ix_actividad_usuario ON actividad_reciente (usuario_id, tipo, ocurrido_en);
CREATE INDEX ix_actividad_fecha ON actividad_reciente (ocurrido_en);

-- Ráfagas detectadas: alguien comentó, solicitó o valoró más de lo normal en pocos minutos.
CREATE TABLE alerta (
    id              UUID          PRIMARY KEY,
    usuario_id      VARCHAR(100)  NOT NULL,
    tipo            VARCHAR(40)   NOT NULL,
    cantidad        INTEGER       NOT NULL,
    ventana_minutos INTEGER       NOT NULL,
    recurso_id      VARCHAR(100),
    desde           TIMESTAMPTZ   NOT NULL,
    hasta           TIMESTAMPTZ   NOT NULL,
    creada_en       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    revisada        BOOLEAN       NOT NULL DEFAULT FALSE
);
CREATE INDEX ix_alerta_usuario ON alerta (usuario_id, tipo, hasta);
CREATE INDEX ix_alerta_creada ON alerta (creada_en DESC);

-- Eventos por hora, por tópico y por tipo: el gráfico de actividad de Administración.
CREATE TABLE metrica_hora (
    hora            TIMESTAMPTZ   NOT NULL,
    topico          VARCHAR(20)   NOT NULL,
    tipo            VARCHAR(40)   NOT NULL,
    cantidad        BIGINT        NOT NULL,
    PRIMARY KEY (hora, topico, tipo)
);

-- Qué consumidor ya procesó qué registro. Kafka entrega al menos una vez: sin esto, un registro
-- reentregado (por un rebalanceo o un reintento) se contaría dos veces.
CREATE TABLE evento_procesado (
    id_evento       VARCHAR(100)  NOT NULL,
    consumidor      VARCHAR(40)   NOT NULL,
    procesado_en    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id_evento, consumidor)
);
