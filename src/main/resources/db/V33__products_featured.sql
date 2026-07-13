ALTER TABLE products
    ADD COLUMN is_featured TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN featured_order INT NULL;

CREATE INDEX idx_products_featured ON products (is_featured, featured_order);
