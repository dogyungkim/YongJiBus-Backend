ALTER TABLE member
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';

CREATE TABLE place (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL,
    address_text VARCHAR(255) NOT NULL,
    latitude DECIMAL(10,7) NOT NULL,
    longitude DECIMAL(11,7) NOT NULL,
    juso_building_management_number VARCHAR(25) NOT NULL,
    category VARCHAR(20) NOT NULL,
    subcategory VARCHAR(30),
    kakao_place_id VARCHAR(32) NOT NULL,
    kakao_place_url VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_by BIGINT,
    approved_by BIGINT,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    approved_at DATETIME(6),
    CONSTRAINT uk_place_kakao_place_id UNIQUE (kakao_place_id),
    CONSTRAINT fk_place_created_by FOREIGN KEY (created_by) REFERENCES member(id) ON DELETE SET NULL,
    CONSTRAINT fk_place_approved_by FOREIGN KEY (approved_by) REFERENCES member(id) ON DELETE SET NULL,
    INDEX idx_place_status_category (status, category)
);

CREATE TABLE place_review (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    place_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    rating INT NOT NULL,
    comment VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL,
    approved_by BIGINT,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    approved_at DATETIME(6),
    CONSTRAINT uk_place_review_member UNIQUE (place_id, member_id),
    CONSTRAINT chk_place_review_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT fk_place_review_place FOREIGN KEY (place_id) REFERENCES place(id) ON DELETE CASCADE,
    CONSTRAINT fk_place_review_member FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE,
    CONSTRAINT fk_place_review_approved_by FOREIGN KEY (approved_by) REFERENCES member(id) ON DELETE SET NULL,
    INDEX idx_place_review_place_status (place_id, status)
);
