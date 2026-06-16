ALTER TABLE banners ADD COLUMN category_id INT NULL;
ALTER TABLE banners ADD CONSTRAINT fk_banners_category FOREIGN KEY (category_id) REFERENCES product_categories(id) ON DELETE CASCADE;
