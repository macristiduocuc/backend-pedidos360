-- ============================================================
-- Último stock conocido de cada producto, recibido desde Kafka (tópico inventario.stock).
-- Una fila por producto: cada mensaje nuevo reemplaza al anterior (el tópico está compactado).
-- Es idempotente: se puede ejecutar varias veces sin romper nada.
-- ============================================================

CREATE TABLE IF NOT EXISTS stock_actual (
    producto_id      BIGINT PRIMARY KEY,
    nombre_producto  VARCHAR(150),
    local_id         BIGINT,
    stock            INT NOT NULL,
    ocurrido_en      TIMESTAMPTZ,
    actualizado_en   TIMESTAMPTZ NOT NULL DEFAULT now()
);
