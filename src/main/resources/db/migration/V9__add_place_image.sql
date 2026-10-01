CREATE TABLE place_image (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    place_id BIGINT NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL,
    created_at DATETIME(6),
    CONSTRAINT uk_place_image_storage_key UNIQUE (storage_key),
    CONSTRAINT uk_place_image_sort_order UNIQUE (place_id, sort_order),
    CONSTRAINT chk_place_image_sort_order CHECK (sort_order BETWEEN 0 AND 4),
    CONSTRAINT fk_place_image_place FOREIGN KEY (place_id) REFERENCES place(id) ON DELETE CASCADE
);
