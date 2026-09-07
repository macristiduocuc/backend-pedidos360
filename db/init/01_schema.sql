-- ============================================================
-- Pedidos 360 · Esquema base (PostgreSQL)
-- Cubre: locales, categorías, productos, pedidos e items,
-- y el log de auditoría que ya simulamos en el frontend.
-- ============================================================

CREATE TABLE locales (
    id              BIGSERIAL PRIMARY KEY,
    nombre          VARCHAR(100) NOT NULL UNIQUE,
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE categorias (
    id              BIGSERIAL PRIMARY KEY,
    nombre          VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE productos (
    id              BIGSERIAL PRIMARY KEY,
    nombre          VARCHAR(150) NOT NULL,
    descripcion     TEXT,
    precio          NUMERIC(10,2) NOT NULL CHECK (precio >= 0),
    stock           INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
    imagen_url      TEXT,
    categoria_id    BIGINT NOT NULL REFERENCES categorias(id),
    local_id        BIGINT NOT NULL REFERENCES locales(id),
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_productos_categoria ON productos(categoria_id);
CREATE INDEX idx_productos_local ON productos(local_id);

CREATE TABLE pedidos (
    id                  BIGSERIAL PRIMARY KEY,
    cliente_nombre      VARCHAR(150) NOT NULL,
    cliente_correo      VARCHAR(150) NOT NULL,
    local_id            BIGINT NOT NULL REFERENCES locales(id),
    modalidad           VARCHAR(30) NOT NULL CHECK (modalidad IN ('Retiro en tienda', 'Despacho a domicilio')),
    direccion_despacho  TEXT,
    estado              VARCHAR(30) NOT NULL DEFAULT 'Pendiente'
                        CHECK (estado IN ('Pendiente','En preparación','Hecho','Entregado','Cancelado')),
    total               NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (total >= 0),
    creado_en           TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_pedidos_estado ON pedidos(estado);
CREATE INDEX idx_pedidos_local ON pedidos(local_id);

-- Snapshot de cada línea del pedido: guardamos nombre y precio en el
-- momento de la compra, para que un cambio futuro en "productos" no
-- altere el historial de pedidos ya hechos.
CREATE TABLE pedido_items (
    id                  BIGSERIAL PRIMARY KEY,
    pedido_id           BIGINT NOT NULL REFERENCES pedidos(id) ON DELETE CASCADE,
    producto_id         BIGINT NOT NULL REFERENCES productos(id),
    nombre_producto     VARCHAR(150) NOT NULL,
    cantidad            INTEGER NOT NULL CHECK (cantidad > 0),
    precio_unitario     NUMERIC(10,2) NOT NULL CHECK (precio_unitario >= 0)
);

CREATE INDEX idx_pedido_items_pedido ON pedido_items(pedido_id);

-- Trazabilidad: quién hizo qué y cuándo (lo que ve el rol Auditoría).
CREATE TABLE eventos_auditoria (
    id              BIGSERIAL PRIMARY KEY,
    pedido_id       BIGINT REFERENCES pedidos(id) ON DELETE SET NULL,
    descripcion     TEXT NOT NULL,
    rol             VARCHAR(30) NOT NULL,
    fecha           TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Mantiene actualizado_en al día cada vez que cambia el estado de un pedido.
CREATE OR REPLACE FUNCTION actualizar_timestamp_pedido()
RETURNS TRIGGER AS $$
BEGIN
    NEW.actualizado_en = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_actualizar_timestamp_pedido
    BEFORE UPDATE ON pedidos
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_timestamp_pedido();
