-- MySQL 8 / local development only.
-- Re-running this file keeps existing dummy places, images, reviews, and members unchanged,
-- and only adds missing dummy data. First run:
-- ./src/main/resources/db/dummy/prepare_place_dummy_images.sh

START TRANSACTION;

INSERT IGNORE INTO
    member (
        name,
        username,
        password,
        email,
        role,
        is_deleted,
        created_at,
        updated_at
    )
VALUES (
        '더미관리자',
        'dummyadmin',
        '$2y$10$p.Di2iPuh6UjBbnC1w2vpegBi.ct.ig9TB/.40Gyr6M6B0GE2JseG',
        'dummy-admin@mju.kr',
        'OPERATOR',
        FALSE,
        NOW(6),
        NOW(6)
    ),
    (
        '더미사용자',
        'dummyuser',
        '$2y$10$p.Di2iPuh6UjBbnC1w2vpegBi.ct.ig9TB/.40Gyr6M6B0GE2JseG',
        'dummy-user@mju.kr',
        'USER',
        FALSE,
        NOW(6),
        NOW(6)
    );

INSERT IGNORE INTO
    member (
        name,
        username,
        password,
        email,
        role,
        is_deleted,
        created_at,
        updated_at
    )
WITH RECURSIVE
    reviewer_seq (n) AS (
        SELECT 1
        UNION ALL
        SELECT n + 1
        FROM reviewer_seq
        WHERE
            n < 24
    )
SELECT CONCAT('더미리뷰어', LPAD(n, 2, '0')), CONCAT('dummyrev', LPAD(n, 2, '0')), '$2y$10$p.Di2iPuh6UjBbnC1w2vpegBi.ct.ig9TB/.40Gyr6M6B0GE2JseG', CONCAT(
        'dummyrev', LPAD(n, 2, '0'), '@mju.kr'
    ), 'USER', FALSE, NOW(6), NOW(6)
FROM reviewer_seq;

SET
    @dummy_operator_id = (
        SELECT id
        FROM member
        WHERE
            email = 'dummy-admin@mju.kr'
        LIMIT 1
    );

SET
    @dummy_user_id = (
        SELECT id
        FROM member
        WHERE
            email = 'dummy-user@mju.kr'
        LIMIT 1
    );

INSERT INTO
    place (
        display_name,
        address_text,
        latitude,
        longitude,
        juso_building_management_number,
        category,
        subcategory,
        kakao_place_id,
        kakao_place_url,
        status,
        created_by,
        approved_by,
        created_at,
        updated_at,
        approved_at
    )
WITH RECURSIVE
    seq (n) AS (
        SELECT 1
        UNION ALL
        SELECT n + 1
        FROM seq
        WHERE
            n < 100
    ),
    dummy AS (
        SELECT
            n,
            CONCAT('990000000', LPAD(n, 3, '0')) AS kakao_id,
            CASE
                WHEN MOD(n, 10) = 0 THEN 'BAR'
                WHEN MOD(n, 10) IN (1, 2) THEN 'CAFE'
                ELSE 'FOOD'
            END AS category,
            CASE MOD(n, 8)
                WHEN 0 THEN 'KOREAN'
                WHEN 1 THEN 'CHINESE'
                WHEN 2 THEN 'JAPANESE'
                WHEN 3 THEN 'WESTERN'
                WHEN 4 THEN 'SNACK'
                WHEN 5 THEN 'CHICKEN'
                WHEN 6 THEN 'FAST_FOOD'
                ELSE 'OTHER'
            END AS food_subcategory,
            CASE
                WHEN MOD(n, 20) < 14 THEN 'APPROVED'
                WHEN MOD(n, 20) < 17 THEN 'PENDING'
                WHEN MOD(n, 20) < 19 THEN 'REJECTED'
                ELSE 'HIDDEN'
            END AS status
        FROM seq
    )
SELECT
    CONCAT(
        '[더미] ',
        CASE
            WHEN dummy.category = 'CAFE' THEN '캠퍼스 카페'
            WHEN dummy.category = 'BAR' THEN '정문 펍'
            ELSE CASE dummy.food_subcategory
                WHEN 'KOREAN' THEN '한식당'
                WHEN 'CHINESE' THEN '중식당'
                WHEN 'JAPANESE' THEN '일식당'
                WHEN 'WESTERN' THEN '양식당'
                WHEN 'SNACK' THEN '분식집'
                WHEN 'CHICKEN' THEN '치킨집'
                WHEN 'FAST_FOOD' THEN '패스트푸드점'
                ELSE '기타 음식점'
            END
        END,
        ' ',
        LPAD(dummy.n, 3, '0')
    ),
    CONCAT(
        '경기도 용인시 처인구 명지로 ',
        100 + MOD(dummy.n * 2, 80)
    ),
    CAST(
        37.2142 + dummy.n * 0.00018 AS DECIMAL(10, 7)
    ),
    CAST(
        127.17766 + MOD(dummy.n * 7, 100) * 0.00020 AS DECIMAL(11, 7)
    ),
    LPAD(
        CONCAT('990', dummy.n),
        25,
        '0'
    ),
    dummy.category,
    CASE
        WHEN dummy.category = 'FOOD' THEN dummy.food_subcategory
        ELSE NULL
    END,
    dummy.kakao_id,
    CONCAT(
        'https://place.map.kakao.com/',
        dummy.kakao_id
    ),
    dummy.status,
    @dummy_operator_id,
    CASE
        WHEN dummy.status = 'APPROVED' THEN @dummy_operator_id
        ELSE NULL
    END,
    TIMESTAMPADD(DAY, dummy.n - 101, NOW(6)),
    TIMESTAMPADD(DAY, dummy.n - 101, NOW(6)),
    CASE
        WHEN dummy.status = 'APPROVED' THEN TIMESTAMPADD(DAY, dummy.n - 101, NOW(6))
        ELSE NULL
    END
FROM dummy
    LEFT JOIN place existing ON existing.kakao_place_id = dummy.kakao_id
WHERE
    existing.id IS NULL;

-- Attach a display image and thumbnail to 80% of dummy places. Legacy-style dummy
-- rows reuse the same key for both variants and therefore need only one file.
INSERT INTO
    place_image (
        place_id,
        storage_key,
        thumbnail_storage_key,
        sort_order,
        created_at
    )
SELECT place.id, CONCAT(
        '00000000-0000-4000-8000-', LPAD(
            CAST(
                RIGHT(place.kakao_place_id, 3) AS UNSIGNED
            ), 12, '0'
        ), '.jpg'
    ), CONCAT(
        '00000000-0000-4000-8000-', LPAD(
            CAST(
                RIGHT(place.kakao_place_id, 3) AS UNSIGNED
            ), 12, '0'
        ), '.jpg'
    ), 0, place.created_at
FROM
    place
    LEFT JOIN place_image existing_image ON existing_image.place_id = place.id
    AND existing_image.sort_order = 0
WHERE
    place.kakao_place_id LIKE '990000000%'
    AND CHAR_LENGTH(place.kakao_place_id) = 12
    AND MOD(
        CAST(
            RIGHT(place.kakao_place_id, 3) AS UNSIGNED
        ),
        5
    ) <> 0
    AND existing_image.id IS NULL;

INSERT INTO
    place_review (
        place_id,
        member_id,
        rating,
        comment,
        status,
        approved_by,
        created_at,
        updated_at,
        approved_at
    )
SELECT
    place.id,
    @dummy_user_id,
    1 + MOD(
        CAST(
            RIGHT(place.kakao_place_id, 3) AS UNSIGNED
        ),
        5
    ),
    CASE MOD(
            CAST(
                RIGHT(place.kakao_place_id, 3) AS UNSIGNED
            ),
            5
        )
        WHEN 0 THEN '가격과 맛 모두 만족스러워요.'
        WHEN 1 THEN '가볍게 들르기 괜찮아요.'
        WHEN 2 THEN '분위기가 편하고 접근성이 좋아요.'
        WHEN 3 THEN '메뉴가 다양해서 다시 방문하고 싶어요.'
        ELSE '수업 사이에 이용하기 편리해요.'
    END,
    'APPROVED',
    @dummy_operator_id,
    TIMESTAMPADD(HOUR, 1, place.created_at),
    TIMESTAMPADD(HOUR, 1, place.created_at),
    TIMESTAMPADD(HOUR, 1, place.created_at)
FROM
    place
    LEFT JOIN place_review existing_review ON existing_review.place_id = place.id
    AND existing_review.member_id = @dummy_user_id
WHERE
    place.kakao_place_id LIKE '990000000%'
    AND CHAR_LENGTH(place.kakao_place_id) = 12
    AND place.status = 'APPROVED'
    AND existing_review.id IS NULL;

INSERT INTO
    place_review (
        place_id,
        member_id,
        rating,
        comment,
        status,
        approved_by,
        created_at,
        updated_at,
        approved_at
    )
WITH
    dummy_place AS (
        SELECT id, created_at, CAST(
                RIGHT(kakao_place_id, 3) AS UNSIGNED
            ) AS place_no
        FROM place
        WHERE
            kakao_place_id LIKE '990000000%'
            AND CHAR_LENGTH(kakao_place_id) = 12
            AND status = 'APPROVED'
    ),
    dummy_reviewer AS (
        SELECT id, CAST(
                RIGHT(username, 2) AS UNSIGNED
            ) AS reviewer_no
        FROM member
        WHERE
            username REGEXP '^dummyrev(0[1-9]|1[0-9]|2[0-4])$'
            AND email = CONCAT(username, '@mju.kr')
            AND role = 'USER'
            AND is_deleted = FALSE
    )
SELECT
    dummy_place.id,
    dummy_reviewer.id,
    1 + MOD(
        dummy_place.place_no + dummy_reviewer.reviewer_no,
        5
    ),
    CASE MOD(
            dummy_place.place_no + dummy_reviewer.reviewer_no,
            24
        )
        WHEN 0 THEN '맛이 깔끔하고 재방문하고 싶어요.'
        WHEN 1 THEN '양이 넉넉해서 든든하게 먹었습니다.'
        WHEN 2 THEN '메뉴 구성이 알차고 선택하기 편해요.'
        WHEN 3 THEN '음식이 따뜻하게 나와서 좋았어요.'
        WHEN 4 THEN '가격 대비 만족도가 높은 곳이에요.'
        WHEN 5 THEN '혼밥하기 편한 분위기였어요.'
        WHEN 6 THEN '친구와 이야기 나누기 좋은 곳이에요.'
        WHEN 7 THEN '수업 전후로 들르기 딱 좋습니다.'
        WHEN 8 THEN '직원분들이 친절해서 기분 좋았어요.'
        WHEN 9 THEN '기다린 보람이 있을 만큼 맛있어요.'
        WHEN 10 THEN '재료가 신선한 느낌이라 만족했어요.'
        WHEN 11 THEN '간이 세지 않아 부담 없이 먹었어요.'
        WHEN 12 THEN '커피 향이 좋고 분위기도 편안해요.'
        WHEN 13 THEN '메뉴가 다양해서 다음엔 다른 걸 먹어볼게요.'
        WHEN 14 THEN '빠르게 준비되어 바쁠 때 유용해요.'
        WHEN 15 THEN '가볍게 먹기 좋고 맛도 괜찮았어요.'
        WHEN 16 THEN '식사 후에도 깔끔한 여운이 남아요.'
        WHEN 17 THEN '매장이 쾌적해서 오래 머물기 좋았어요.'
        WHEN 18 THEN '처음 방문했는데 기대 이상이었어요.'
        WHEN 19 THEN '든든한 한 끼로 추천하고 싶습니다.'
        WHEN 20 THEN '소스와 재료 조합이 잘 어울렸어요.'
        WHEN 21 THEN '가격이 합리적이라 자주 찾을 것 같아요.'
        WHEN 22 THEN '메뉴 사진과 실제 음식이 비슷했어요.'
        ELSE '주변에서 편하게 들를 만한 맛집이에요.'
    END,
    'APPROVED',
    @dummy_operator_id,
    TIMESTAMPADD(
        MINUTE,
        60 + dummy_reviewer.reviewer_no,
        dummy_place.created_at
    ),
    TIMESTAMPADD(
        MINUTE,
        60 + dummy_reviewer.reviewer_no,
        dummy_place.created_at
    ),
    TIMESTAMPADD(
        MINUTE,
        60 + dummy_reviewer.reviewer_no,
        dummy_place.created_at
    )
FROM
    dummy_place
    CROSS JOIN dummy_reviewer
    LEFT JOIN place_review existing_review ON existing_review.place_id = dummy_place.id
    AND existing_review.member_id = dummy_reviewer.id
WHERE
    existing_review.id IS NULL;

COMMIT;

SELECT
    COUNT(*) AS dummy_place_count,
    SUM(status = 'APPROVED') AS approved_count,
    SUM(status = 'PENDING') AS pending_count,
    SUM(status = 'REJECTED') AS rejected_count,
    SUM(status = 'HIDDEN') AS hidden_count
FROM place
WHERE
    kakao_place_id LIKE '990000000%'
    AND CHAR_LENGTH(kakao_place_id) = 12;

SELECT COUNT(*) AS dummy_place_thumbnail_count
FROM place_image
    JOIN place ON place.id = place_image.place_id
WHERE
    place.kakao_place_id LIKE '990000000%'
    AND CHAR_LENGTH(place.kakao_place_id) = 12
    AND place_image.sort_order = 0;

SELECT dummy_place.kakao_place_id, COUNT(place_review.id) AS approved_review_count
FROM
    place dummy_place
    LEFT JOIN place_review ON place_review.place_id = dummy_place.id
    AND place_review.status = 'APPROVED'
WHERE
    dummy_place.kakao_place_id LIKE '990000000%'
    AND CHAR_LENGTH(dummy_place.kakao_place_id) = 12
    AND dummy_place.status = 'APPROVED'
GROUP BY
    dummy_place.id,
    dummy_place.kakao_place_id
ORDER BY dummy_place.id;

-- 필요할 때만 수동 정리:
-- DELETE FROM place_image WHERE place_id IN (SELECT id FROM place WHERE kakao_place_id LIKE '990000000%');
-- DELETE FROM place_review WHERE place_id IN (SELECT id FROM place WHERE kakao_place_id LIKE '990000000%');
-- DELETE FROM place WHERE kakao_place_id LIKE '990000000%';
-- 위 리뷰와 장소를 정리한 뒤 추가 리뷰어 회원을 정리:
-- DELETE FROM member WHERE username REGEXP '^dummyrev(0[1-9]|1[0-9]|2[0-4])$';