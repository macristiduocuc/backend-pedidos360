-- ============================================================
-- Datos de ejemplo, alineados con el catálogo mock del frontend
-- ============================================================

INSERT INTO categorias (nombre) VALUES
    ('Panadería'),
    ('Pastelería'),
    ('Cafetería');

INSERT INTO locales (nombre) VALUES
    ('Dulce Trigo'),
    ('Café Central'),
    ('Horno de Barrio'),
    ('Aroma & Miga'),
    ('Mesón del Café'),
    ('Pan Artesanal');

INSERT INTO productos (nombre, descripcion, precio, stock, imagen_url, categoria_id, local_id) VALUES
    ('Torta Tres Leches', 'Bizcocho bañado en tres leches con merengue suizo (15 personas).', 18900, 5,
        'https://images.unsplash.com/photo-1464349095431-e9a21285b5f3?auto=format&fit=crop&w=500&q=80',
        (SELECT id FROM categorias WHERE nombre = 'Pastelería'),
        (SELECT id FROM locales WHERE nombre = 'Dulce Trigo')),

    ('Café Americano', 'Café de grano recién molido, tostado medio.', 1800, 100,
        'https://images.unsplash.com/photo-1559525839-b184a4d698c7?auto=format&fit=crop&w=500&q=80',
        (SELECT id FROM categorias WHERE nombre = 'Cafetería'),
        (SELECT id FROM locales WHERE nombre = 'Café Central')),

    ('Marraqueta x4', 'Pan de trigo recién horneado, crocante por fuera.', 1600, 80,
        'https://images.unsplash.com/photo-1549931319-a545749fcd15?auto=format&fit=crop&w=500&q=80',
        (SELECT id FROM categorias WHERE nombre = 'Panadería'),
        (SELECT id FROM locales WHERE nombre = 'Horno de Barrio')),

    ('Kuchen de Manzana', 'Kuchen alemán con manzanas caramelizadas.', 6500, 12,
        'https://images.unsplash.com/photo-1568571780765-9276ac8b75a2?auto=format&fit=crop&w=500&q=80',
        (SELECT id FROM categorias WHERE nombre = 'Pastelería'),
        (SELECT id FROM locales WHERE nombre = 'Aroma & Miga')),

    ('Capuccino', 'Espresso con leche vaporizada y espuma cremosa.', 2200, 100,
        'https://images.unsplash.com/photo-1572442388796-11668a67e53d?auto=format&fit=crop&w=500&q=80',
        (SELECT id FROM categorias WHERE nombre = 'Cafetería'),
        (SELECT id FROM locales WHERE nombre = 'Mesón del Café')),

    ('Hallulla x6', 'Pan plano tradicional, recién salido del horno.', 1400, 90,
        'https://images.unsplash.com/photo-1585478259715-4d3a5f3b9f0a?auto=format&fit=crop&w=500&q=80',
        (SELECT id FROM categorias WHERE nombre = 'Panadería'),
        (SELECT id FROM locales WHERE nombre = 'Pan Artesanal'));
