ALTER TABLE banners
    ADD COLUMN object_position VARCHAR(100) NOT NULL DEFAULT 'center' AFTER position;

UPDATE banners
SET object_position = 'center'
WHERE object_position IS NULL OR object_position = '';
