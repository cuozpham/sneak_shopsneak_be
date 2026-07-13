CREATE INDEX idx_products_rating ON products (rating_average, review_count);
CREATE INDEX idx_products_created_at ON products (created_at);
