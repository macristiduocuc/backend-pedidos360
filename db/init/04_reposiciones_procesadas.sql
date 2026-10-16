-- ============================================================
-- Registro de pedidos cancelados cuyo stock ya fue repuesto.
-- Evita reponer dos veces si RabbitMQ entrega el mismo mensaje duplicado.
-- Es idempotente: se puede ejecutar varias veces sin romper nada.
-- ============================================================

CREATE TABLE IF NOT EXISTS reposiciones_procesadas (
    pedido_id     BIGINT PRIMARY KEY,
    procesado_en  TIMESTAMPTZ NOT NULL DEFAULT now()
);