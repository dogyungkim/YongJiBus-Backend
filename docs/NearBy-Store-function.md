# 주변 기능 백엔드 설계

- 문서 상태: 구현 기준안
- 대상 저장소: `YongJiBus-Backend`
- 작성일: 2026-09-10
- 검토 반영일: 2026-09-15
- 연관 문서: [주변 기능 프론트엔드 설계](./nearby_frontend_design.md)
- 이미지 API 연동: [장소 이미지 API](./nearby-place-image-api.md)

## 1. 목적

학교 생활권의 장소, 최대 5장의 장소 이미지와 사용자 평가를 관리하는 REST API를 제공한다. 업로드한 이미지 원본은 보관하지 않고 표시용 최적화 이미지와 목록용 썸네일만 저장한다. 운영자는 장소를 직접 등록하고 사용자 요청을 승인할 수 있으며, 로그인 사용자는 카카오맵에서 실제 장소를 선택한 후 장소 등록과 평가를 요청할 수 있다.

장소는 음식점만이 아니라 카페와 술집을 포함하므로 도메인 이름은 `restaurant`가 아닌 `place`를 사용한다.

## 2. 현재 백엔드 기준

현재 저장소의 다음 기술과 패턴을 그대로 사용한다.

- Java 17
- Spring Boot 3.3.4
- Spring MVC, Spring Security, Spring Data JPA
- MySQL 8.0
- Flyway
- `RestClient`
- JWT 인증과 `MemberDetail`
- `YongJiResponse<T>` 공통 응답
- `SliceResponse<T>` 페이지 응답
- `ErrorCode`와 `GlobalExceptionHandler`
- JPA Auditing

UTM-K 좌표를 WGS84로 안전하게 변환하기 위해 `org.locationtech.proj4j:proj4j`를 사용한다. EPSG 데이터 모듈은 추가하지 않고 EPSG:5179와 EPSG:4326의 Proj.4 파라미터를 코드에 명시한다.

이미지 축소와 JPEG 품질 제어에는 [`net.coobird:thumbnailator:0.4.21`](https://github.com/coobird/thumbnailator), JDK 17이 기본 지원하지 않는 WebP 입력 디코딩에는 [`com.twelvemonkeys.imageio:imageio-webp:3.15.0`](https://github.com/haraldk/TwelveMonkeys)을 사용한다. 출력은 JDK ImageIO가 기본 지원하는 JPEG로 통일하므로 WebP 인코더는 추가하지 않는다. 그 외에 새 ORM, 지도 라이브러리, 캐시 서버, 별도 관리자 백엔드는 추가하지 않는다.

## 3. 핵심 설계 원칙

### 3.1 카카오, 공공주소, 우리 데이터를 분리한다

카카오 Local API 응답에서 받은 이름, 주소, 전화번호, 좌표, 카테고리를 DB에 자동 저장하지 않는다. 카카오 측 공식 답변상 장소 ID와 랜딩 URL은 저장할 수 있지만 나머지 필드는 실시간 호출로 사용해야 한다.

- 정책 답변: [카카오맵 API 호출과 장소 저장](https://devtalk.kakao.com/t/api/150642)
- 추가 사례: [로컬 API 응답 데이터 저장 범위](https://devtalk.kakao.com/t/api/148194)

저장할 주소와 좌표는 행정안전부 주소기반산업지원서비스의 도로명주소·좌표제공 API에서 다시 확인한다. 카카오 검색 결과를 공공주소 검색어로 사용할 수는 있지만, 카카오가 반환한 주소와 좌표를 그대로 저장하지 않는다.

따라서 DB에는 출처가 구분된 다음 데이터만 저장한다.

1. 카카오 식별 정보: 중복 장소 확인용 `kakaoPlaceId`, 상세 화면 연결용 `kakaoPlaceUrl`
2. 공공주소 정보: `jusoBuildingManagementNumber`, 도로명주소, 공공 좌표를 WGS84로 변환한 위도·경도
3. 우리 서비스 정보: 사용자가 입력하고 운영자가 확정한 표시 이름과 카테고리, 승인 상태, 사용자 평가

카카오 검색 응답 전체는 검색 요청의 HTTP 응답으로만 전달한다. 선택된 장소의 ID·URL·도로명주소·지번주소는 짧은 유효기간의 `selectionProof`에 서명해 클라이언트에 전달하고, 등록 요청에서는 원본 필드 대신 이 증명을 받는다. 증명에서 꺼낸 주소는 공공주소 검색 후 폐기하고 DB, Redis, Caffeine, 로그에 남기지 않는다. 공공주소도 전체 데이터를 수집하지 않고 자동으로 확인된 한 건만 저장한다. 운영 적용 전 API 신청 목적에 선택 주소의 영구 저장을 명시하고 당시 이용조건을 확인한다.

### 3.2 공개 조회와 쓰기 권한을 분리한다

- 승인 장소와 승인 평가 조회: 비로그인 허용
- 카카오 장소 검색: 로그인 필요
- 장소 요청과 평가 작성: 로그인 필요
- 승인·거절·숨김과 운영자 직접 등록: `OPERATOR` 권한 필요

### 3.3 데이터베이스 제약으로 중복을 막는다

- 카카오 장소 ID당 장소 하나
- 공공주소 건물관리번호는 건물 식별자이므로 중복을 허용
- 회원 1명당 장소별 평가 하나
- 별점은 1~5점

애플리케이션 사전 검사와 DB 제약을 함께 사용한다. 동시 요청의 최종 방어는 DB 제약이 담당한다.

### 3.4 카카오 선택 결과의 위변조를 막는다

카카오 검색 결과를 그대로 다시 받으면 사용자가 장소 ID와 다른 주소를 조합해 전송할 수 있으므로, 백엔드는 검색 응답마다 `selectionProof`를 발급한다.

- 기존 JJWT의 HMAC-SHA256 서명을 재사용하되 액세스 토큰과 다른 환경변수 키를 사용한다.
- 증명에는 `memberId`, `kakaoPlaceId`, `kakaoPlaceUrl`, 도로명주소, 지번주소, 만료 시각을 포함한다.
- 유효기간은 발급 시점부터 5분이다.
- 등록 요청 회원과 `memberId`가 다르거나 서명 또는 만료 검증에 실패하면 `INVALID_KAKAO_PLACE`를 반환한다.
- 증명은 서버에 저장하지 않으며 검색 결과나 증명 전문을 로그에 남기지 않는다.

이 방식은 검색 결과 캐시 없이 사용자가 실제로 받은 카카오 검색 결과와 등록 요청을 연결한다.

## 4. 도메인 모델

### 4.1 Member 변경

현재 `MemberDetail.getAuthorities()`가 빈 목록을 반환하므로 운영자 권한을 추가한다.

```java
public enum MemberRole {
    USER,
    OPERATOR
}
```

`Member`에 다음 필드를 추가한다.

```text
role: MemberRole, NOT NULL, 기본값 USER
```

`MemberDetail`은 `ROLE_USER` 또는 `ROLE_OPERATOR` 권한을 반환한다. `MemberResponseDTO`에도 `role`을 추가해 별도 운영자 클라이언트가 진입 권한을 판단할 수 있게 한다.

첫 운영자 지정은 관리자 UI를 만들지 않고 운영 DB에서 역할을 한 번 변경한다.

### 4.2 Place

```text
Place
- id
- displayName
- addressText
- latitude
- longitude
- jusoBuildingManagementNumber
- category
- subcategory
- kakaoPlaceId
- kakaoPlaceUrl
- status
- createdBy
- approvedBy
- createdAt
- updatedAt
- approvedAt
```

장소 생성 전에 공공주소와 좌표 확인을 완료한다. `PENDING`을 포함한 모든 Place에는 다음 값이 반드시 존재해야 한다.

- `displayName`
- `addressText`
- `latitude`
- `longitude`
- `jusoBuildingManagementNumber`
- `category`
- `kakaoPlaceId`
- `kakaoPlaceUrl`

카테고리:

```java
public enum PlaceCategory {
    FOOD,
    CAFE,
    BAR
}
```

음식 하위 카테고리:

```java
public enum PlaceSubcategory {
    KOREAN,
    CHINESE,
    JAPANESE,
    WESTERN,
    SNACK,
    CHICKEN,
    FAST_FOOD,
    OTHER
}
```

`category != FOOD`이면 `subcategory`는 `null`이어야 한다. `category == FOOD`이면 하위 카테고리가 필요하다.

상태:

```java
public enum PlaceStatus {
    PENDING,
    APPROVED,
    REJECTED,
    HIDDEN
}
```

허용 전이:

```text
PENDING  -> APPROVED | REJECTED
REJECTED -> APPROVED
APPROVED -> HIDDEN
HIDDEN   -> APPROVED
```

거절된 장소의 저장 주소가 올바른 경우에는 운영자가 표시 이름과 카테고리를 확정해 같은 행을 `APPROVED`로 전환할 수 있다. 초기 버전에서는 기존 Place의 주소·좌표 정정을 지원하지 않는다. 저장 주소가 잘못된 `PENDING` 장소는 거절하고, 이미 승인된 장소는 숨긴 상태로 유지한다.

`APPROVED`로 전환하거나 복원할 때 `approvedBy`와 `approvedAt`을 현재 운영자와 현재 시각으로 기록한다. `APPROVED`에서 `HIDDEN`으로 전환할 때는 두 값을 `null`로 지운다. 즉 `status != APPROVED`인 Place의 두 승인 필드는 항상 `null`이다.

### 4.3 PlaceReview

```text
PlaceReview
- id
- place
- member
- rating
- comment
- status
- approvedBy
- createdAt
- updatedAt
- approvedAt
```

```java
public enum ReviewStatus {
    PENDING,
    APPROVED,
    REJECTED,
    HIDDEN
}
```

허용 전이:

```text
PENDING  -> APPROVED | REJECTED
APPROVED -> PENDING | HIDDEN
REJECTED -> PENDING
HIDDEN   -> APPROVED
```

사용자가 승인 또는 거절된 평가를 수정하면 같은 행의 내용과 별점을 바꾸고 상태를 `PENDING`으로 되돌린다. 이때 `approvedBy`와 `approvedAt`을 `null`로 지운다. `HIDDEN` 평가는 사용자가 수정할 수 없으며 `INVALID_REVIEW_STATUS_TRANSITION`을 반환한다. 운영자가 복원한 뒤에는 다시 수정할 수 있다. 평가 삭제는 물리 삭제한다. 장소를 숨기더라도 평가는 보존하고 공개 조회에서 함께 제외한다.

평가를 `APPROVED`로 전환하거나 복원할 때 `approvedBy`와 `approvedAt`을 현재 운영자와 현재 시각으로 기록한다. 승인 평가를 `HIDDEN`으로 전환하거나 사용자의 수정으로 `PENDING`이 되면 두 값을 `null`로 지운다. 즉 `status != APPROVED`인 PlaceReview의 두 승인 필드는 항상 `null`이다.

### 4.4 PlaceImage

```text
PlaceImage
- id
- place
- storageKey
- thumbnailStorageKey
- sortOrder
- createdAt
```

`storageKey`는 긴 변 최대 2048px인 표시용 JPEG, `thumbnailStorageKey`는 긴 변 최대 480px인 목록용 JPEG를 가리킨다. 클라이언트가 전송한 원본 파일 자체는 저장하지 않는다. 두 키는 저장소가 생성한 서로 다른 UUID 파일명이며 DB에는 이미지 바이트나 전체 공개 URL을 저장하지 않는다.

## 5. 데이터베이스 설계

현재 저장소는 Flyway와 `ddl-auto: validate`를 함께 사용한다. 회원·장소·평가는 `V3__add_place_and_member_role.sql`, 이미지는 `V4__add_place_image.sql`, 썸네일 키는 `V5__add_place_image_thumbnail.sql`에서 추가한다. 애플리케이션 시작 시 Flyway가 마이그레이션을 적용한 다음 Hibernate가 스키마를 검증한다. 운영 DB에 같은 SQL을 별도로 수동 적용하지 않는다. `database_schema.sql`은 신규 DB 생성용 참고본으로 동일하게 갱신하되 Flyway 마이그레이션을 운영 스키마의 기준으로 삼는다.

```sql
ALTER TABLE member
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER'
        COMMENT 'USER 또는 OPERATOR';

CREATE TABLE place (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL,
    address_text VARCHAR(255) NOT NULL,
    latitude DECIMAL(10, 7) NOT NULL,
    longitude DECIMAL(11, 7) NOT NULL,
    juso_building_management_number VARCHAR(25) NOT NULL,
    category VARCHAR(20) NOT NULL,
    subcategory VARCHAR(30),
    kakao_place_id VARCHAR(32) NOT NULL,
    kakao_place_url VARCHAR(512) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_by BIGINT,
    approved_by BIGINT,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    approved_at DATETIME(6),

    CONSTRAINT uk_place_kakao_place_id UNIQUE (kakao_place_id),
    CONSTRAINT fk_place_created_by
        FOREIGN KEY (created_by) REFERENCES member(id),
    CONSTRAINT fk_place_approved_by
        FOREIGN KEY (approved_by) REFERENCES member(id),
    INDEX idx_place_status_category (status, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE place_review (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    place_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    rating TINYINT NOT NULL,
    comment VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_by BIGINT,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    approved_at DATETIME(6),

    CONSTRAINT uk_place_review_member UNIQUE (place_id, member_id),
    CONSTRAINT ck_place_review_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT fk_place_review_place
        FOREIGN KEY (place_id) REFERENCES place(id),
    CONSTRAINT fk_place_review_member
        FOREIGN KEY (member_id) REFERENCES member(id),
    CONSTRAINT fk_place_review_approved_by
        FOREIGN KEY (approved_by) REFERENCES member(id),
    INDEX idx_place_review_place_status (place_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE place_image (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    place_id BIGINT NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    thumbnail_storage_key VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL,
    created_at DATETIME(6),
    CONSTRAINT uk_place_image_storage_key UNIQUE (storage_key),
    CONSTRAINT uk_place_image_thumbnail_storage_key UNIQUE (thumbnail_storage_key),
    CONSTRAINT uk_place_image_sort_order UNIQUE (place_id, sort_order),
    CONSTRAINT chk_place_image_sort_order CHECK (sort_order BETWEEN 0 AND 4),
    CONSTRAINT fk_place_image_place FOREIGN KEY (place_id) REFERENCES place(id) ON DELETE CASCADE
);
```

평균 별점과 평가 수는 저장하지 않는다. 승인 평가를 대상으로 조회 시 계산한다. 성능 문제가 측정되기 전에는 집계 컬럼이나 별도 통계 테이블을 만들지 않는다.

이미지 파일은 DB에 넣지 않고 저장소가 발급한 표시용 이미지 키와 썸네일 키만 저장한다. `sortOrder`가 가장 작은 이미지를 대표 이미지로 사용하며 장소당 5개 제한은 서비스 검증과 `0~4` DB 제약으로 함께 보장한다.

V5 적용 전에 존재한 행은 SQL만으로 썸네일을 생성할 수 없으므로 `thumbnail_storage_key = storage_key`로 채운 뒤 `NOT NULL` 제약을 적용한다. 기존 행은 당분간 대표 이미지 원본을 썸네일로 사용하고, 신규 업로드부터 두 파일을 분리한다. 실제 운영 이미지가 쌓이기 전에는 별도 일괄 변환 작업을 만들지 않는다.

```sql
ALTER TABLE place_image
    ADD COLUMN thumbnail_storage_key VARCHAR(255) NULL AFTER storage_key;

UPDATE place_image
SET thumbnail_storage_key = storage_key
WHERE thumbnail_storage_key IS NULL;

ALTER TABLE place_image
    MODIFY COLUMN thumbnail_storage_key VARCHAR(255) NOT NULL,
    ADD CONSTRAINT uk_place_image_thumbnail_storage_key UNIQUE (thumbnail_storage_key);
```

V5 이후 실행되는 `place_dummy_data.sql`도 `thumbnail_storage_key`를 명시한다. 개발용 기존 이미지는 두 키에 같은 값을 사용해 별도 이미지 생성 도구를 요구하지 않는다.

## 6. 패키지 구조

```text
com.yongjibus.place
├── client
│   ├── KakaoLocalClient
│   └── JusoAddressClient
├── controller
│   ├── PlaceController
│   ├── PlaceReviewController
│   └── AdminPlaceController
├── controller.dto
├── domain
│   ├── Place
│   ├── PlaceImage
│   ├── PlaceReview
│   └── enum types
├── repository
│   ├── PlaceRepository
│   ├── PlaceImageRepository
│   └── PlaceReviewRepository
├── service
    ├── PlaceService
    ├── PlaceImageProcessor
    ├── PlaceImageService
    └── PlaceSelectionProofService
└── storage
    ├── ImageStorage
    └── LocalImageStorage
```

운영자 기능도 동일한 `PlaceService`를 사용한다. 초기 구현에서 일반 서비스와 관리자 서비스를 별도로 나누지 않는다. `PlaceSelectionProofService`는 기존 JJWT로 선택 증명을 발급·검증하는 작은 보안 컴포넌트이며 장소 업무 규칙은 갖지 않는다. `PlaceImageProcessor`는 업로드 바이트의 디코딩·검증·리사이징·JPEG 인코딩만 담당하는 구체 클래스이며 저장소 인터페이스나 별도 팩토리는 만들지 않는다.

## 7. API 공통 규약

모든 응답은 기존 형식을 유지한다.

```json
{
  "status": 200,
  "data": {}
}
```

성공 응답은 기존 `YongJiResponse.success()`에 맞춰 HTTP 200을 사용한다. 날짜와 시간은 ISO 8601 문자열로 반환한다.

JWT 필터의 인증 실패와 Spring Security의 인증·인가 실패도 각각 `AuthenticationEntryPoint`, `AccessDeniedHandler`에서 같은 `YongJiResponse` JSON 형식으로 반환한다. 보안 필터에서 별도의 `{code, message, status}` 응답 형식을 만들지 않는다.

## 8. 공개 API

### 8.1 승인 장소 목록

```http
GET /places
```

인증 없이 접근 가능하다. `APPROVED` 장소만 반환한다. 초기 장소 수가 적으므로 필터와 페이지네이션 없이 전체를 반환한다.

```typescript
type PlaceSummaryDTO = {
  id: number;
  displayName: string;
  addressText: string;
  latitude: number;
  longitude: number;
  category: 'FOOD' | 'CAFE' | 'BAR';
  subcategory: PlaceSubcategory | null;
  averageRating: number | null;
  reviewCount: number;
  representativeReview: string | null;
  kakaoPlaceUrl: string;
  thumbnailUrl: string | null;
};
```

응답 예시:

```json
{
  "status": 200,
  "data": [
    {
      "id": 1,
      "displayName": "장소 이름",
      "addressText": "경기도 용인시 처인구 ...",
      "latitude": 37.2345000,
      "longitude": 127.1889000,
      "category": "FOOD",
      "subcategory": "KOREAN",
      "averageRating": 4.5,
      "reviewCount": 12,
      "representativeReview": "혼밥하기 편하고 양이 넉넉해요.",
      "kakaoPlaceUrl": "https://place.map.kakao.com/123456",
      "thumbnailUrl": "/place-images/550e8400-e29b-41d4-a716-446655440000.jpg"
    }
  ]
}
```

장소와 승인 평가 집계는 한 번의 projection 쿼리로 조회해 N+1을 만들지 않는다. 대표 한줄평은 `approvedAt DESC, id DESC` 기준의 가장 최근 승인 평가를 사용한다.

### 8.2 장소 상세

```http
GET /places/{placeId}
```

승인 장소의 요약 정보와 로그인 사용자의 평가 상태를 반환한다. 비로그인일 때 `myReview`는 `null`이다.

```typescript
type MyPlaceReviewDTO = {
  id: number;
  rating: number;
  comment: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'HIDDEN';
  createdAt: string;
  updatedAt: string;
};

type PlaceDetailDTO = PlaceSummaryDTO & {
  imageUrls: string[];
  myReview: MyPlaceReviewDTO | null;
};

type Response = YongJiResponse<PlaceDetailDTO>;
```

로그인 사용자의 평가는 상태와 관계없이 `myReview`에 반환한다. 따라서 대기·승인·거절·숨김 상태를 모두 확인할 수 있다. 다른 사용자의 비승인 평가는 반환하지 않는다.

### 8.3 승인 평가 목록

```http
GET /places/{placeId}/reviews?page=0&size=20
```

`APPROVED` 평가만 최신순으로 반환한다.

```typescript
type PlaceReviewDTO = {
  id: number;
  username: string;
  rating: number;
  comment: string;
  isMine: boolean;
  createdAt: string;
  updatedAt: string;
};

type Response = YongJiResponse<SliceResponse<PlaceReviewDTO>>;
```

## 9. 회원 API

### 9.1 카카오 장소 검색

```http
GET /places/kakao-search?query={query}
Authorization: Bearer {accessToken}
```

백엔드가 카카오 키워드 장소 검색 API를 실시간 호출한다. 클라이언트가 임의 좌표와 반경을 전달하지 않도록 검색 중심과 반경은 서버 설정으로 고정한다.

기본 설정:

```yaml
nearby:
  search-center-latitude: 37.2242
  search-center-longitude: 127.18766
  search-radius-meters: 5000
  selection-proof-secret: ${NEARBY_SELECTION_PROOF_SECRET}
```

`NEARBY_SELECTION_PROOF_SECRET`은 액세스·리프레시 토큰의 `JWT_SECRET`과 분리한 32바이트 이상의 비밀값으로 관리한다. 증명 유효기간 5분은 초기 고정값으로 두고 별도 설정으로 만들지 않는다.

실제 운영 반경은 초기 등록 결과를 확인한 후 설정값만 조정한다.

카카오 응답에 우리 DB의 장소 상태를 합쳐 반환한다.

```typescript
type KakaoPlaceSearchItem = {
  placeId: string;
  placeName: string;
  categoryName: string;
  roadAddressName: string;
  addressName: string;
  latitude: number;
  longitude: number;
  placeUrl: string;
  registeredPlaceId: number | null;
  registrationStatus: 'PENDING' | 'APPROVED' | 'UNAVAILABLE' | null;
  selectionProof: string;
};
```

기존 Place가 `REJECTED` 또는 `HIDDEN`이면 `registeredPlaceId`는 기존 Place ID를 반환하고 `registrationStatus`는 내부 상태를 구분하지 않는 `UNAVAILABLE`로 반환한다. 클라이언트에는 거절·숨김 사유나 원래 상태를 노출하지 않으며, 두 상태 모두 새 등록 요청을 받을 수 없는 동일한 상태로 취급한다.

카카오 API는 위치·반경 검색과 장소 ID, 주소, 좌표, 상세 URL을 제공한다. [카카오 Local API 문서](https://developers.kakao.com/docs/ko/local/dev-guide)

검색 결과는 등록 폼의 장소 확인에만 사용한다. 백엔드는 카카오가 반환한 장소 ID와 URL의 형식을 검증한 뒤 장소 ID·URL·도로명주소·지번주소·현재 회원 ID·5분 만료 시각을 담은 `selectionProof`를 함께 반환한다. 선택한 결과의 주소는 등록 요청의 증명에서 꺼내 공공주소 검색어로만 사용하며 DB, Redis, Caffeine에 저장하지 않고 로그에도 기록하지 않는다.

### 9.2 장소 등록 및 최초 평가 요청

```http
POST /places/requests
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "selectionProof": "eyJhbGciOiJIUzI1NiJ9...",
  "displayName": "사용자가 입력한 장소 이름",
  "category": "FOOD",
  "subcategory": "KOREAN",
  "rating": 5,
  "comment": "혼밥하기 좋고 음식이 빨리 나와요."
}
```

기존 JSON 요청은 이미지 없이 계속 사용할 수 있다. 이미지를 함께 등록하려면 같은 `POST /places/requests` 경로에 multipart 요청을 보낸다. `request` 파트는 필수 JSON 파트(`Content-Type: application/json`)이며 위 `PlaceRequest` 본문과 동일하다. `images` 파트는 선택 사항이고 같은 이름을 반복해 0~5개 파일을 보낸다. 이미지가 없으면 `images` 파트를 생략한다.

```http
POST /places/requests
Authorization: Bearer {accessToken}
Content-Type: multipart/form-data
```

```bash
curl -X POST "https://api.example.com/places/requests" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -F 'request={"selectionProof":"eyJhbGciOiJIUzI1NiJ9...","displayName":"학생회관 카페","category":"CAFE","subcategory":null,"rating":5,"comment":"분위기가 좋아요."};type=application/json' \
  -F "images=@./cafe.png;type=image/png" \
  -F "images=@./interior.webp;type=image/webp"
```

프론트엔드 `FormData` 예시:

```ts
const form = new FormData();
form.append("request", new Blob([JSON.stringify(request)], { type: "application/json" }));
for (const file of files) {
  form.append("images", file, file.name);
}

const response = await fetch("/places/requests", {
  method: "POST",
  headers: { Authorization: `Bearer ${accessToken}` },
  body: form,
});
```

`FormData` 요청에는 `Content-Type`을 직접 지정하지 않는다. 브라우저가 multipart boundary를 설정한다. 사용자 이미지는 `PENDING` 장소에 등록할 때만 첨부할 수 있고, 장소가 승인·거절·숨김 상태가 된 뒤 사용자가 이미지만 추가하거나 삭제하는 standalone API는 없다. 장소와 평가 DB 요청을 먼저 저장한 뒤 이미지 첨부를 수행하므로 이미지 처리·저장이 실패해도 장소 요청과 평가는 접수된 상태로 남을 수 있다. 같은 요청을 다시 보내면 평가를 upsert한 뒤 이미지 첨부를 다시 시도한다. 성공 응답은 이미지 목록이 아니라 기존 `PlaceRequestResult`다.

서버는 `selectionProof`의 서명·만료·회원 일치를 검증해 카카오 장소 ID·URL·주소를 복원한다. 증명 안의 도로명주소가 비어 있으면 지번주소를 사용한다. 두 주소가 모두 비어 있으면 요청을 거절한다. 두 값은 공공주소 검색에만 사용하고 저장하지 않는다.

처리 규칙:

1. `selectionProof`의 서명·만료·요청 회원 일치 여부 검증
2. 증명에서 복원한 카카오 장소 ID 형식과 URL 호스트·경로 검증
3. 카카오 장소 ID로 기존 Place 조회
4. 기존 Place가 `REJECTED` 또는 `HIDDEN`이면 요청 거절
5. 기존 Place가 `PENDING` 또는 `APPROVED`면 공공주소를 다시 조회하지 않고 요청자의 평가만 생성 또는 갱신
6. 기존 Place가 없으면 증명의 카카오 도로명주소, 지번주소 순서로 공공주소 검색
7. 정규화한 주소가 정확히 일치하는 결과 한 건을 선택하고, 없거나 여러 건이면 요청 거절
8. 공공주소 결과의 식별값으로 좌표제공 API 호출
9. 건물관리번호 일치 여부 확인 및 UTM-K 좌표를 WGS84로 변환
10. 공공 API가 반환한 주소·건물관리번호·좌표만 `PENDING` Place에 저장
11. 요청자의 평가를 `PENDING`으로 생성
12. Place와 평가 저장은 하나의 트랜잭션으로 처리

응답:

```typescript
type PlaceRequestResultDTO = {
  placeId: number;
  placeStatus: 'PENDING' | 'APPROVED';
  reviewStatus: 'PENDING';
};
```

### 9.3 내 평가 작성 또는 수정

```http
PUT /places/{placeId}/reviews/me
Authorization: Bearer {accessToken}
```

```json
{
  "rating": 4,
  "comment": "가격이 괜찮고 단체로 방문하기 좋아요."
}
```

- 승인 장소에만 작성 가능
- 신규 작성과 수정 모두 동일 엔드포인트 사용
- 기존 행이 있으면 갱신하고 `PENDING`으로 전환
- 승인 전에는 공개 평균과 평가 목록에 반영하지 않음

### 9.4 내 평가 삭제

```http
DELETE /places/{placeId}/reviews/me
Authorization: Bearer {accessToken}
```

자신의 평가만 삭제할 수 있다.

## 10. 운영자 API

모든 엔드포인트는 `ROLE_OPERATOR`가 필요하다.

```typescript
type AdminMemberSummaryDTO = {
  id: number;
  username: string;
};

type AdminPlaceDTO = {
  id: number;
  displayName: string;
  addressText: string;
  latitude: number;
  longitude: number;
  jusoBuildingManagementNumber: string;
  category: 'FOOD' | 'CAFE' | 'BAR';
  subcategory: PlaceSubcategory | null;
  kakaoPlaceId: string;
  kakaoPlaceUrl: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'HIDDEN';
  createdBy: AdminMemberSummaryDTO | null;
  approvedBy: AdminMemberSummaryDTO | null;
  createdAt: string;
  updatedAt: string;
  approvedAt: string | null;
  images: {
    id: number;
    imageUrl: string;
    thumbnailUrl: string;
    sortOrder: number;
  }[];
};

type AdminReviewDTO = {
  id: number;
  placeId: number;
  placeDisplayName: string;
  member: AdminMemberSummaryDTO;
  rating: number;
  comment: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'HIDDEN';
  approvedBy: AdminMemberSummaryDTO | null;
  createdAt: string;
  updatedAt: string;
  approvedAt: string | null;
};
```

운영자 목록과 상태 변경 API는 모두 위 DTO를 사용한다. 상태 변경 성공 시 문자열이나 ID만 반환하지 않고 변경이 반영된 전체 DTO를 반환한다.

### 10.1 장소 직접 등록

```http
POST /admin/places
Authorization: Bearer {operatorToken}
```

```json
{
  "displayName": "장소 이름",
  "category": "FOOD",
  "subcategory": "KOREAN",
  "selectionProof": "eyJhbGciOiJIUzI1NiJ9..."
}
```

운영자 직접 등록도 로그인한 운영자가 카카오 검색에서 발급받은 `selectionProof`를 사용한다. 백엔드는 증명을 검증하고 공공주소를 다시 조회해 주소와 좌표를 확정한다. 같은 카카오 장소 ID의 Place가 상태와 관계없이 이미 있으면 `409 CONFLICT`를 반환하며 기존 행의 주소·좌표를 덮어쓰지 않는다. 신규 등록은 즉시 `APPROVED`로 생성하면서 현재 운영자와 현재 시각을 승인 정보로 기록하고 `YongJiResponse<AdminPlaceDTO>`로 반환한다.

### 10.2 심사 목록

```http
GET /admin/places?status=PENDING&page=0&size=20
GET /admin/reviews?status=PENDING&page=0&size=20
```

장소 목록은 `YongJiResponse<SliceResponse<AdminPlaceDTO>>`, 평가 목록은 `YongJiResponse<SliceResponse<AdminReviewDTO>>`로 반환한다.

### 10.3 장소 승인

```http
POST /admin/places/{placeId}/approve
```

```json
{
  "displayName": "확정된 표시 이름",
  "category": "FOOD",
  "subcategory": "KOREAN"
}
```

승인 시 운영자는 카카오 상세 링크와 저장된 공공주소가 같은 장소인지 확인하고 표시 이름과 카테고리를 확정한다. 주소가 잘못됐다면 승인하지 않는다. `PENDING`과 `REJECTED` 장소를 승인할 수 있으며, 장소 승인만으로 연결된 평가를 자동 승인하지 않는다.

응답은 변경된 `YongJiResponse<AdminPlaceDTO>`다.

### 10.4 장소 거절·숨김·복원

```http
POST /admin/places/{placeId}/reject
POST /admin/places/{placeId}/hide
POST /admin/places/{placeId}/restore
```

장소 요청을 거절하면 해당 장소의 `PENDING` 평가도 함께 `REJECTED`로 변경한다. 장소 숨김은 평가 상태를 바꾸지 않는다.

각 응답은 변경된 `YongJiResponse<AdminPlaceDTO>`다.

### 10.5 평가 승인·거절·숨김·복원

```http
POST /admin/reviews/{reviewId}/approve
POST /admin/reviews/{reviewId}/reject
POST /admin/reviews/{reviewId}/hide
POST /admin/reviews/{reviewId}/restore
```

장소가 `APPROVED`가 아니면 평가를 승인할 수 없다.

각 응답은 변경된 `YongJiResponse<AdminReviewDTO>`다.

### 10.6 장소 이미지 등록·삭제

```http
POST /admin/places/{placeId}/images
Authorization: Bearer {operatorToken}
Content-Type: multipart/form-data
images: 최대 5개의 파일

DELETE /admin/places/{placeId}/images/{imageId}
Authorization: Bearer {operatorToken}
```

JPEG, PNG, WEBP만 허용하며 파일당 최대 5MB, 장소당 최대 5개다. 업로드 순서대로 빈 `sortOrder`를 배정하고 가장 낮은 순서를 대표 이미지로 사용한다. 파일명과 MIME 값만 믿지 않고 파일 시그니처와 실제 디코딩 결과를 함께 확인한다.

파일별 처리는 다음 순서로 동기 실행한다.

1. MIME, 파일 시그니처, 5MB 제한을 검사한다.
2. 전체 픽셀을 메모리에 올리기 전에 `ImageReader`로 가로·세로를 읽고 긴 변 12,000px 이하, 총 25MP 이하인지 검사한다. 곱셈은 `long`으로 계산한다.
3. 이미지를 한 번 디코딩하고 JPEG의 EXIF 방향 정보가 있으면 회전을 적용한다.
4. 비율을 유지하면서 긴 변 최대 2048px로 축소하고 JPEG 품질 85%로 표시용 이미지를 만든다. 작은 이미지는 확대하지 않는다.
5. 표시용 이미지에서 긴 변 최대 480px, JPEG 품질 75%인 썸네일을 만든다. 서버에서는 자르지 않고 화면이 필요하면 `object-fit: cover`를 사용한다.
6. PNG와 WebP의 투명 영역은 흰색 배경에 합성하고 두 결과를 모두 `.jpg`로 저장한다. 메타데이터는 출력에 복사하지 않는다.
7. 표시용 키와 썸네일 키를 `place_image`에 기록하고 업로드 원본 바이트는 폐기한다.

처리는 파일 한 장씩 순차 수행하고 디코딩한 이미지를 요청 전체에 보관하지 않는다. 관리자와 사용자 등록 요청의 저빈도 업로드이므로 별도 작업 큐나 비동기 상태 API는 만들지 않는다.

업로드 응답의 이미지 항목은 다음과 같다.

```typescript
type PlaceImageItem = {
  id: number;
  imageUrl: string;       // 최대 2048px 표시용 이미지
  thumbnailUrl: string;   // 최대 480px 목록용 이미지
  sortOrder: number;
};
```

공개 장소 목록의 `thumbnailUrl`은 대표 `PlaceImageItem.thumbnailUrl`, 장소 상세의 `imageUrls`는 각 `PlaceImageItem.imageUrl`을 사용한다. 로컬 이미지는 인증 없이 `GET /place-images/{storageKey}`로 제공한다. 이미지 삭제 시 표시용 파일과 썸네일 파일을 모두 삭제하며, V5 이전 행처럼 두 키가 같으면 한 번만 삭제한다.

### 10.7 이미지 저장소

초기 구현은 로컬 디스크를 사용한다.

```yaml
storage:
  image:
    type: ${IMAGE_STORAGE_TYPE:local}
    local:
      root: ${IMAGE_STORAGE_LOCAL_ROOT:./data/place-images}
      public-path: ${IMAGE_STORAGE_PUBLIC_PATH:/place-images}
```

이미지 처리기는 파일 시스템을 직접 사용하지 않는다. 처리 결과가 이미 메모리의 제한된 JPEG 바이트이므로 `ImageStorage`는 다음 세 작업만 제공한다.

```java
String store(byte[] content, String extension);
void delete(String storageKey);
String publicUrl(String storageKey);
```

`LocalImageStorage`는 표시용과 썸네일 모두 원본 파일명과 무관한 UUID `.jpg` 키로 같은 영속 디렉터리에 저장한다. 저장 중 실패한 파일과 DB 기록에 실패한 파일은 생성된 키 목록으로 최선 삭제한다. 프로세스가 파일 저장 직후 강제 종료되어 생기는 고아 파일을 위한 outbox나 정리 배치는 실제 문제가 확인되기 전에는 추가하지 않는다.

S3 전환 시 이미지 처리 로직과 DB 구조는 바꾸지 않고 `ImageStorage` 구현체만 추가한다. 기존 키의 파일을 S3에 복사하고 `IMAGE_STORAGE_TYPE`을 변경하며, S3 구현체의 `publicUrl`은 버킷 또는 CDN 주소를 반환한다. 운영 컨테이너에서 로컬 저장을 사용할 때는 기본 `/data/place-images`를 영속 볼륨에 마운트해야 한다.

## 11. 외부 API 연동

### 11.1 카카오 구성

기존 `HolidayApiClient`처럼 Spring `RestClient`를 사용한다.

```yaml
kakao:
  rest-api-key: ${KAKAO_REST_API_KEY}
```

호출:

```http
GET https://dapi.kakao.com/v2/local/search/keyword.json
Authorization: KakaoAK {KAKAO_REST_API_KEY}
```

필수 파라미터:

- `query`
- `x`: 설정된 학교 중심 경도
- `y`: 설정된 학교 중심 위도
- `radius`: 설정된 검색 반경
- `size=15`
- `sort=accuracy`

REST API 키는 환경변수로만 주입하고 로그에 출력하지 않는다. 운영 환경에서는 카카오 개발자 콘솔의 호출 허용 IP에 백엔드 서버 IP를 등록한다.

### 11.2 카카오 장애 처리

- 연결·읽기 제한시간: 3초
- 카카오 4xx/5xx, 타임아웃, 잘못된 응답: `PLACE_SEARCH_UNAVAILABLE`
- 빈 결과: 정상 응답 `[]`
- 쿼터 초과: 사용자에게 검색 불가 메시지를 반환하고 운영 로그에 오류 코드만 기록
- 재시도: 요청 안에서 자동 재시도하지 않음

사용자가 직접 다시 시도할 수 있으므로 서버 재시도로 쿼터를 이중 소비하지 않는다.

### 11.3 카카오 호출량 보호

검색 API는 로그인 사용자에게만 제공한다. 회원당 분당 10회 제한을 적용하며, 이미 설치된 Caffeine으로 짧은 fixed-window 카운터만 둔다. 카카오 검색 결과 자체는 캐시하지 않는다.

카카오의 현재 키워드 장소 검색 무료 쿼터는 일 100,000건이며, 무료 쿼터는 개발자 계정에서 첫 번째로 활성화한 카카오맵 앱에만 적용된다. 배포 전에 콘솔에서 무료 쿼터 대상 여부를 확인한다. [카카오 쿼터 문서](https://developers.kakao.com/docs/ko/getting-started/quota)

### 11.4 공공주소 구성

행정안전부 주소기반산업지원서비스의 도로명주소 검색 API와 좌표제공 API를 사용한다. 두 API의 승인키 유형이 다를 수 있으므로 각각 환경변수로 관리한다.

```yaml
juso:
  search-confirmation-key: ${JUSO_SEARCH_API_KEY}
  coordinate-confirmation-key: ${JUSO_COORDINATE_API_KEY}
```

호출:

```http
GET https://business.juso.go.kr/addrlink/addrLinkApi.do
GET https://business.juso.go.kr/addrlink/addrCoordApi.do
```

검증한 `selectionProof`의 카카오 도로명주소를 우선 검색하고 결과가 없으면 지번주소를 사용한다. 공백, 괄호 안 건물명 등 표기 차이를 정규화한 뒤 정확히 일치하는 결과 한 건에서 행정구역코드, 도로명코드, 지하여부, 건물본번, 건물부번과 건물관리번호를 추출한다. 그 한 건에 대해서만 좌표제공 API를 호출한다.

좌표제공 API의 `entX`, `entY`는 UTM-K(GRS80) 좌표이므로 Proj4j로 WGS84 경도·위도 순서로 변환한다. 변환 결과가 대한민국 경계와 학교 생활권 설정 범위 안인지 검사한다. 공공주소 정책과 좌표계 안내는 [주소기반산업지원서비스](https://business.juso.go.kr/)와 [좌표제공 API 공식 답변](https://business.juso.go.kr/addrlink/qna/qnaDetail.do?bulletinRefSn=128137&currentPage=64&keyword=&noticeMgtSn=128137&noticeType=QNA&noticeTypeTmp=QNA&page=&searchType=)을 따른다.

### 11.5 공공주소 장애와 호출량 처리

- 연결·읽기 제한시간: 3초
- 검색 API 오류: `PUBLIC_ADDRESS_SEARCH_UNAVAILABLE`
- 좌표 누락 또는 건물관리번호 불일치: `PUBLIC_ADDRESS_INVALID`
- 좌표 API 오류: `PUBLIC_ADDRESS_RESOLVE_UNAVAILABLE`
- 빈 검색 결과 또는 정확한 단일 결과 없음: `PUBLIC_ADDRESS_NOT_RESOLVED`
- 자동 재시도: 없음

좌표제공 API는 공식 안내 기준 5초당 10건을 넘지 않도록 서버 전체 호출 제한을 두고, 신규 장소 요청 한 건당 한 번만 호출한다. 공공주소 확인에 실패하면 장소와 평가를 저장하지 않으며, 이미 승인된 장소 조회는 공공주소 API 장애와 무관하게 DB 데이터로 제공한다.

검색 결과 전문과 승인키는 로그에 남기지 않는다. 장애 진단에는 주소값 없이 작업·실패 사유, 도로명/지번 값 존재 여부, 결과·일치 건수와 비밀이 아닌 HTTP/API 오류 코드만 남길 수 있다. API 신청 시 선택된 장소의 주소·좌표 저장 목적을 명시하고 운영 배포 전에 승인 범위와 최신 이용조건을 기록한다.

## 12. 조회 구현

### 12.1 장소 목록

한 쿼리에서 다음 값을 조회하는 DTO projection을 사용한다.

- Place 기본 필드
- 승인 평가 평균
- 승인 평가 수
- `approvedAt DESC, id DESC` 기준 최근 승인 한줄평

평균 계산은 `ROUND(AVG(rating), 1)` 기준으로 반환한다. 승인 평가가 없으면 평균은 `null`, 개수는 `0`이다.

초기 버전은 장소 전체를 반환하며 정렬과 필터는 앱에서 수행한다. 공개 목록은 운영자가 관리하는 학교 생활권 장소만 포함하므로 서버 위치 검색이나 공간 인덱스는 만들지 않는다.

### 12.2 평가 목록

- 조건: 장소 `APPROVED`, 평가 `APPROVED`
- 정렬: `createdAt DESC, id DESC`
- 기본 크기: 20
- 최대 크기: 50

## 13. 트랜잭션과 동시성

다음 서비스 메서드에 `@Transactional`을 적용한다.

- 내 평가 작성·수정·삭제
- 운영자 장소 승인·거절·숨김·복원
- 운영자 평가 승인·거절·숨김·복원

장소 등록 요청은 외부 API 호출과 중복 충돌 재시도가 있으므로 메서드 전체에 `@Transactional`을 붙이지 않는다. 공공주소 조회와 좌표 변환을 먼저 완료한 뒤, `PlaceService`가 `TransactionTemplate`로 다음 저장 블록만 트랜잭션으로 실행한다.

1. 카카오 장소 ID로 Place를 다시 조회하고 현재 상태를 다시 검증한다.
2. Place가 없으면 검증된 공공주소 결과로 생성한다.
3. `(place_id, member_id)`로 평가를 조회해 생성하거나 내용과 상태를 갱신한다.
4. Place와 평가를 `saveAndFlush`해 유니크 제약 위반을 트랜잭션 경계 안에서 확인한다.

동시에 같은 장소나 같은 회원의 평가 요청이 충돌하면 첫 트랜잭션을 완전히 롤백한 뒤 새 `TransactionTemplate` 트랜잭션에서 위 저장 블록 전체를 한 번만 다시 실행한다. `uk_place_kakao_place_id`와 `uk_place_review_member` 충돌만 재시도하며 다른 무결성 오류는 감추지 않는다. 두 번째 충돌은 `PLACE_REQUEST_CONFLICT`로 반환한다. 다른 요청이 먼저 Place를 생성했다면 미리 조회한 공공주소 결과는 버리고 기존 Place에 평가만 연결한다.

이 구조로 외부 API를 기다리는 동안 DB 연결을 점유하지 않으면서 신규 Place와 최초 평가의 원자성을 유지한다.

이미지 업로드도 파일 디코딩·변환·저장을 수행하는 동안 DB 트랜잭션을 열어두지 않는다. `PlaceImageService`는 기존 `TransactionTemplate` 패턴을 재사용해 다음과 같이 처리한다.

1. 장소 존재 여부와 현재 이미지 수를 먼저 확인한다.
2. 업로드 파일을 한 장씩 변환하고 표시용·썸네일 파일을 저장한다.
3. 짧은 트랜잭션에서 이미지 목록을 다시 조회해 5개 제한과 빈 `sortOrder`를 재확인하고 `saveAndFlush`한다.
4. 변환, 파일 저장, 동시 업로드 재검사 또는 DB 저장이 실패하면 이번 요청에서 생성한 두 종류의 파일을 모두 최선 삭제한다.

파일 시스템과 DB는 단일 트랜잭션으로 묶을 수 없으므로 프로세스 강제 종료 순간의 고아 파일 가능성은 남는다. 초기 운영에서는 별도 outbox를 만들지 않고, 실제 고아 파일이 측정될 때 저장 키와 DB 키를 비교하는 정리 작업을 추가한다.

사용자 장소 요청의 이미지 최종 저장에서는 `PENDING` 장소 행을 `PlaceRepository.findByIdForUpdate`로 읽어 승인과 이미지 삽입이 겹치지 않게 한다. 이 경로 외의 장소·이미지 작업에는 비관적 락을 추가하지 않으며, 다른 충돌이 측정될 때만 검토한다.

## 14. 입력 검증

| 필드 | 규칙 |
| --- | --- |
| 카카오 검색어 | 공백 제거 후 2~50자 |
| 선택 증명 | 필수, 최대 2048자, HMAC 서명·만료·회원 일치 검증 |
| 표시 이름 | 사용자가 직접 입력, 공백 제거 후 1~100자 |
| 증명 내 카카오 도로명주소 | 최대 255자, 지번주소와 둘 중 하나 필수 |
| 증명 내 카카오 지번주소 | 최대 255자, 도로명주소와 둘 중 하나 필수 |
| 주소 | 공공주소 API 응답, 1~255자 |
| 위도 | 공공 좌표 변환값, -90 이상 90 이하 |
| 경도 | 공공 좌표 변환값, -180 이상 180 이하 |
| 카카오 장소 ID | 숫자로만 구성, 최대 32자 |
| 카카오 URL | `http` 또는 `https`, 호스트 `place.map.kakao.com`, 경로 `/{동일 ID}`, 사용자정보·포트·쿼리·프래그먼트 없음. 검증 후 `https://place.map.kakao.com/{ID}`로 정규화 |
| 별점 | 1~5 정수 |
| 한줄평 | 공백 제거 후 1~120자 |
| 음식 하위 카테고리 | `FOOD`일 때 필수 |
| 장소 이미지 | JPEG/PNG/WEBP, 파일당 5MB 이하, 장소당 최대 5개, 긴 변 12,000px 이하, 총 25MP 이하, 파일 시그니처와 실제 디코딩 검증 |

HTML 태그를 별도로 허용하지 않는다. 응답은 JSON 문자열로 반환하고 앱은 텍스트 컴포넌트로만 렌더링한다.

장소 등록·수정 DTO에는 `kakaoPlaceId`, `kakaoPlaceUrl`, 카카오 주소, `addressText`, `latitude`, `longitude`를 두지 않고 `selectionProof`만 받는다. 증명에서 복원한 카카오 주소는 공공주소 검색어로만 사용하고, 서버가 공공 API에서 확인한 주소·건물관리번호·좌표만 저장한다. 공공 API 응답의 건물관리번호는 숫자 25자인지 추가 검증한다.

## 15. 보안 설정

JWT 필터는 경로별 인증 필요 여부를 판단하지 않는다. `/auth/token/refresh`의 리프레시 토큰 처리는 유지하되 일반 요청은 다음 규칙만 수행한다.

단, 기존 `POST /gmail/bounce`는 애플리케이션 JWT가 아닌 Google Pub/Sub의 Bearer 토큰을 사용하므로 JWT 필터를 통과시키고 엔드포인트의 `GmailPubSubTokenVerifier`가 인증한다. 이 예외는 장소 API의 공개·인증 경로 판정에는 사용하지 않는다.

1. `Authorization` 헤더가 없으면 인증을 만들지 않고 필터 체인을 계속한다.
2. Bearer 액세스 토큰이 있으면 검증 후 `MemberDetail` 인증을 설정한다.
3. 헤더 형식이나 토큰이 잘못됐으면 `YongJiResponse` 형식의 401을 반환한다.

엔드포인트의 인증·인가는 `SecurityConfig` 한 곳에서 결정한다. HTTP 메서드별 규칙은 첫 번째 일치 규칙이 적용되므로 카카오 검색의 인증 규칙을 가변 경로 공개 규칙보다 먼저 둔다.

```text
GET /places/kakao-search    authenticated  # 반드시 공개 가변 경로보다 먼저 선언
GET /places                 permitAll
GET /places/{id}            permitAll
GET /places/{id}/reviews    permitAll
GET /place-images/**        permitAll
그 외 /places/**            authenticated
/admin/**                   ROLE_OPERATOR
```

공개 상세와 평가 목록은 토큰 없이도 접근할 수 있고, 유효한 토큰이 함께 오면 인증 정보를 사용해 `myReview`와 `isMine`을 계산한다.

`@EnableMethodSecurity`를 활성화하고 `AdminPlaceController`에 다음을 적용한다.

```java
@PreAuthorize("hasRole('OPERATOR')")
```

운영자 권한은 요청 DTO나 헤더 값으로 판단하지 않고 JWT로 조회한 현재 Member의 역할만 사용한다. 인증이 없을 때의 401은 `AuthenticationEntryPoint`, 권한이 부족할 때의 403은 `AccessDeniedHandler`에서 `YongJiResponse`로 직렬화한다.

사용자의 정밀 위치는 앱에서 거리 계산하므로 백엔드로 받거나 저장하지 않는다. Place의 주소·좌표 필드는 클라이언트에서 받지 않고 공공 API 결과만 저장한다. 카카오 주소는 검증한 `selectionProof`에서만 꺼내 공공주소 검색어로 사용하고 카카오 검색 중심은 서버의 고정 학교 좌표로 유지한다.

## 16. 오류 코드

기존 `ErrorCode`에 다음 값을 추가한다.

```text
PLACE_NOT_FOUND                 404
PLACE_ALREADY_EXISTS           409
PLACE_REQUEST_CONFLICT         409
PLACE_NOT_APPROVED              400
PLACE_REQUEST_NOT_ALLOWED       409
REVIEW_NOT_FOUND                404
INVALID_RATING                  400
INVALID_PLACE_CATEGORY          400
INVALID_KAKAO_PLACE             400
PLACE_SEARCH_UNAVAILABLE        503
PLACE_SEARCH_RATE_LIMITED       429
PUBLIC_ADDRESS_INVALID          400
PUBLIC_ADDRESS_NOT_RESOLVED     422
PUBLIC_ADDRESS_SEARCH_UNAVAILABLE 503
PUBLIC_ADDRESS_RESOLVE_UNAVAILABLE 503
OPERATOR_REQUIRED               403
INVALID_PLACE_STATUS_TRANSITION 409
INVALID_REVIEW_STATUS_TRANSITION 409
INVALID_PLACE_IMAGE              400
TOO_MANY_PLACE_IMAGES            400
PLACE_IMAGE_NOT_FOUND            404
PLACE_IMAGE_STORAGE_FAILED       500
```

`PlaceException` 하나로 장소와 평가 도메인 오류를 전달한다. 평가 전용 예외 클래스를 별도로 만들지 않는다.

## 17. 로그와 관측성

- 카카오 검색 성공/실패 건수
- 공공주소 검색·좌표 확인 성공/실패 건수
- 장소 요청·승인·거절 건수
- 평가 요청·승인·거절 건수
- 이미지 변환 성공·실패 건수와 처리 시간
- 공개 장소 조회 실패

기존 Micrometer/Actuator를 재사용하고 새 모니터링 의존성을 추가하지 않는다.

로그에 남기지 않는 값:

- 카카오 REST API 키
- 카카오 검색 결과 전문
- 카카오 `selectionProof` 전문
- 장소 요청의 카카오 주소 검색어
- 공공주소 API 승인키와 검색 결과 전문
- 사용자 한줄평 전문
- 업로드 이미지 원본 바이트와 원본 파일명
- JWT

공공주소 장애 진단 로그에는 주소값 없이 작업·실패 사유, 도로명/지번 값 존재 여부, 결과·일치 건수와 비밀이 아닌 HTTP/API 오류 코드만 남길 수 있다.

운영자 상태 변경 로그에는 대상 ID, 이전 상태, 새 상태, 운영자 ID만 남긴다.

## 18. 테스트 계획

### 18.1 도메인 테스트

- 음식 카테고리에서 하위 카테고리 필수
- 카페·술집에서 하위 카테고리 거부
- 장소와 평가 상태 전이
- 장소·평가 승인 시 승인자와 승인 시각 설정, 비승인 상태 전환 시 초기화
- 모든 장소의 공공주소 필수 필드
- 평가 수정 시 `PENDING` 전환
- 숨김 평가의 사용자 수정 거부
- UTM-K(GRS80)에서 WGS84로 변환한 좌표의 허용 오차

### 18.2 저장소 테스트

- 카카오 장소 ID 중복 방지
- 장소·회원 평가 중복 방지
- 승인 평가만 평균과 개수에 반영
- 대표 한줄평을 `approvedAt DESC, id DESC`로 선택
- 평가 없는 장소의 평균 `null`
- 승인 장소만 공개 목록에 포함
- 평가 페이지 정렬

### 18.3 서비스 테스트

- 신규 장소와 최초 평가를 한 트랜잭션에서 생성
- 카카오 검색에서 발급한 `selectionProof`로만 장소 요청 가능
- 변조·만료되거나 다른 회원에게 발급된 `selectionProof` 거부
- 신규 장소 요청은 카카오 주소로 공공주소를 자동 확인하고 클라이언트 좌표를 받지 않음
- 공공주소가 없거나 여러 건이면 장소와 평가를 저장하지 않음
- 공공주소 건물관리번호 불일치 시 저장 거부
- 기존 승인 장소 요청 시 장소 중복 없이 평가만 upsert
- 카카오 검색에서 기존 `REJECTED`·`HIDDEN` 장소를 모두 `UNAVAILABLE`로 반환
- 운영자 직접 등록에서 모든 상태의 기존 카카오 장소 ID 중복 거부
- 같은 요청 재전송 시 중복 평가 미생성
- 장소·평가 유니크 충돌 시 첫 트랜잭션 롤백 후 새 트랜잭션에서 한 번만 재시도
- 재시도 중 먼저 생성된 Place가 있으면 사전 조회한 공공주소 결과를 저장하지 않음
- 장소 거절 시 연결된 대기 평가 거절
- 숨긴 장소가 공개 조회에서 제외
- 일반 사용자의 운영자 작업 거부
- 장소 이미지 5개 제한, 형식·크기·해상도·파일 시그니처 검증
- JPEG·PNG·WebP 입력을 표시용 JPEG와 썸네일 JPEG로 변환
- EXIF 방향 적용, 투명 영역 흰색 합성, 작은 이미지 확대 방지
- 표시용 긴 변 2048px 이하와 썸네일 긴 변 480px 이하
- 이미지 목록의 대표 이미지와 표시 순서
- 공개 목록은 썸네일 URL, 상세는 표시용 이미지 URL 사용
- 로컬 표시용·썸네일 이미지 저장·조회·동시 삭제
- 두 번째 파일 저장 또는 DB 저장 실패 시 이번 요청의 파일 전체 정리

### 18.4 컨트롤러 테스트

- 공개 조회 비인증 접근
- 검색·등록·평가의 비인증 접근 거부
- 운영자 API의 역할 검사
- 카카오 검색 응답이 `REJECTED`·`HIDDEN`을 구분하지 않고 `UNAVAILABLE`로 노출
- 공개 장소 상세·평가 목록에서 토큰이 없으면 익명 응답, 유효한 토큰이 있으면 `myReview`·`isMine` 반영
- 장소 상세에서 내 평가의 모든 상태를 반환하고 다른 사용자의 비승인 평가는 제외
- 운영자 목록과 상태 변경 응답이 전체 `AdminPlaceDTO` 또는 `AdminReviewDTO`인지 확인
- 인증·인가 실패가 `YongJiResponse` 형식인지 확인
- Bean Validation 오류가 기존 `YongJiResponse` 형식인지 확인
- 카카오 장애가 503으로 변환되는지 확인
- 장소 등록 중 공공주소 검색·좌표 확인 장애가 503으로 변환되는지 확인

### 18.5 외부 API 테스트

`MockRestServiceServer`로 카카오와 공공주소 `RestClient`를 검증한다.

- 카카오 Authorization 헤더와 검색 중심·반경·크기 파라미터
- 공공주소 승인키와 주소 식별 파라미터
- 두 공급자의 정상 JSON 매핑과 빈 결과
- 좌표 응답의 건물관리번호 일치 검사
- 타임아웃과 4xx/5xx 오류 변환

현재 Flyway 마이그레이션 테스트를 V5 기준으로 갱신하고 빈 DB와 기존 스키마 모두에서 V5 적용 후 Hibernate `validate`가 성공하는지 확인한다. V4의 기존 이미지 행은 V5 적용 후 `thumbnail_storage_key = storage_key`인지 확인한다. 운영자 권한과 공개 API의 선택적 인증 테스트는 standalone MockMvc가 아니라 실제 Spring Security 필터 체인을 포함한 통합 테스트 한 개로 검증한다.

최종 검증 명령:

```bash
./gradlew test
```

## 19. 배포 순서

1. V3·V4·V5 Flyway 마이그레이션과 갱신한 `database_schema.sql` 검토
2. 공공주소 API 신청 목적과 주소·좌표 저장 범위를 서면 확인
3. `JUSO_SEARCH_API_KEY`, `JUSO_COORDINATE_API_KEY` 설정
4. `KAKAO_REST_API_KEY`, `NEARBY_SELECTION_PROOF_SECRET`, 검색 범위 설정
5. 카카오 콘솔에서 API 활성화, 쿼터 대상, 허용 IP 확인
6. `IMAGE_STORAGE_LOCAL_ROOT`를 영속 볼륨에 연결
7. 백엔드 배포 시 Flyway V5 자동 적용 및 Hibernate 스키마 검증 확인
8. 기존 회원 역할이 `USER`인지 확인
9. 지정 회원 1명을 `OPERATOR`로 변경
10. 공개/인증/운영자 API 점검
11. 운영자가 초기 장소 등록
12. 프론트엔드 `주변` 탭 배포

백엔드를 먼저 배포하면 구버전 앱에는 영향이 없다. 프론트엔드 문제 발생 시 탭을 제거한 버전으로 롤백하고 장소·평가 데이터는 보존한다.

## 20. 완료 조건

- 운영자와 일반 사용자의 권한이 서버에서 구분된다.
- 운영자는 장소를 직접 등록하고 요청을 승인·거절할 수 있다.
- 로그인 사용자는 카카오 장소를 선택한 후 별도 공공주소 선택 없이 장소 요청과 최초 평가를 제출할 수 있다.
- 장소 등록은 로그인 회원에게 발급된 유효한 `selectionProof`로만 가능하다.
- 카카오 장소 ID와 사용자별 평가 중복이 DB에서 차단된다.
- 주소·좌표와 건물관리번호는 공공주소 API 응답에서만 저장된다.
- 클라이언트가 장소 주소나 좌표를 임의로 저장할 수 없다.
- 승인 장소와 승인 평가만 공개 API에 포함된다.
- 평균 별점과 평가 수가 승인 평가만으로 계산된다.
- 카카오 장소 ID와 URL 외 검색 원본 데이터가 DB나 캐시에 저장되지 않는다.
- 카카오 또는 공공주소 API 장애가 기존 장소 조회를 막지 않는다.
- 운영 환경 스키마와 `database_schema.sql`이 일치한다.
- 운영자가 장소당 최대 5개의 이미지를 등록·삭제할 수 있다.
- 업로드 원본은 보관하지 않고 최대 2048px 표시용 JPEG와 최대 480px 썸네일 JPEG만 저장한다.
- 목록은 대표 이미지의 썸네일, 상세는 전체 표시용 이미지를 순서대로 반환한다.
- 표시용·썸네일 파일은 로컬 영속 볼륨에 저장되고 DB에는 두 저장소 키만 저장된다.
- 운영 환경의 Flyway 버전이 V5 이상이다.

## 21. 후속 범위

다음 항목은 초기 구현에 포함하지 않는다.

- 즐겨찾기
- 평가 댓글과 대댓글
- 평가 신고
- 승인 결과 알림과 내 요청 이력
- 서버 거리 검색과 공간 인덱스
- 평균 별점 비정규화
- 업로드 원본 보관과 원본 다운로드
- 비동기 이미지 처리 큐와 별도 이미지 처리 서버
- 기존 이미지의 썸네일 일괄 생성
- 별도 관리자 웹 애플리케이션
- 네이버맵 또는 복수 장소 공급자 추상화
- 기존 Place의 주소·좌표 정정
