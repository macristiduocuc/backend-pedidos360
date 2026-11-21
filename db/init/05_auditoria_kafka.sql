-- ============================================================
-- Historial de eventos de pedidos recibidos desde Kafka (auditoría).
-- Cada fila guarda de qué partición y offset vino, así que un mismo mensaje no se registra dos veces.
-- Es idempotente: se puede ejecutar varias veces sin romper nada.
-- ============================================================

CREATE TABLE IF NOT EXISTS auditoria_kafka (
    id               BIGSERIAL PRIMARY KEY,
    topico           VARCHAR(249) NOT NULL,
    particion        INT NOT NULL,
    offset_registro  BIGINT NOT NULL,
    clave            VARCHAR(100),
    tipo             VARCHAR(30) NOT NULL,
    pedido_id        BIGINT NOT NULL,
    local_id         BIGINT,
    estado           VARCHAR(30),
    total            NUMERIC(12,2),
    ocurrido_en      TIMESTAMPTZ,
    recibido_en      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT auditoria_kafka_registro_unico UNIQUE (topico, particion, offset_registro)
);

CREATE INDEX IF NOT EXISTS idx_auditoria_kafka_pedido ON auditoria_kafka(pedido_id);
