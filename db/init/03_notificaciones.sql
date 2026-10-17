-- ============================================================
-- Notificaciones generadas por los consumidores de RabbitMQ.
-- Un registro va dirigido a un cliente (correo) o a un rol (ej.: AUDITORIA).
-- Es idempotente: se puede ejecutar varias veces sin romper nada.
-- ============================================================

CREATE TABLE IF NOT EXISTS notificaciones (
    id                   BIGSERIAL PRIMARY KEY,
    pedido_id            BIGINT REFERENCES pedidos(id) ON DELETE SET NULL,
    destinatario_correo  VARCHAR(150),
    destinatario_rol     VARCHAR(30),
    mensaje              VARCHAR(300) NOT NULL,
    leida                BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT notificaciones_destinatario_chk
        CHECK (destinatario_correo IS NOT NULL OR destinatario_rol IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS idx_notificaciones_correo ON notificaciones(destinatario_correo, leida);
CREATE INDEX IF NOT EXISTS idx_notificaciones_rol ON notificaciones(destinatario_rol, leida);