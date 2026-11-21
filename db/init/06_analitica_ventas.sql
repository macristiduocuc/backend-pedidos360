-- ============================================================
-- Analítica de ventas calculada desde los eventos de pedidos de Kafka.
--  - ventas_registradas: pedidos ya contados (evita contar dos veces un mismo pedido)
--  - ventas_por_producto: unidades vendidas por producto (ya descontadas las cancelaciones)
-- Es idempotente: se puede ejecutar varias veces sin romper nada.
-- ============================================================

CREATE TABLE IF NOT EXISTS ventas_registradas (
    pedido_id      BIGINT PRIMARY KEY,
    cancelada      BOOLEAN NOT NULL DEFAULT FALSE,
    registrada_en  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS ventas_por_producto (
    producto_id        BIGINT PRIMARY KEY,
    unidades_vendidas  BIGINT NOT NULL DEFAULT 0,
    actualizado_en     TIMESTAMPTZ NOT NULL DEFAULT now()
);
