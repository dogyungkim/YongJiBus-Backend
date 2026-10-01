ALTER TABLE place_image
    ADD COLUMN thumbnail_storage_key VARCHAR(255) NULL AFTER storage_key;

UPDATE place_image
SET thumbnail_storage_key = storage_key
WHERE thumbnail_storage_key IS NULL;

ALTER TABLE place_image
    MODIFY COLUMN thumbnail_storage_key VARCHAR(255) NOT NULL;

ALTER TABLE place_image
    ADD CONSTRAINT uk_place_image_thumbnail_storage_key UNIQUE (thumbnail_storage_key);
