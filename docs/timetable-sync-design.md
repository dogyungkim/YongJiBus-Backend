# 시간표 동기화 백엔드 설계 체크리스트

- 문서 상태: 백엔드 구현 완료 (배포 확인 전)
- 대상 저장소: `YongJiBus-Backend`
- 작성일: 2026-09-19
- 연관 문서: [시간표 동기화 프론트엔드 설계](../../Yongji-RN/docs/timetable-sync-design.md)

## 1. 목적

시간표의 원본을 백엔드에서 관리하고 앱 배포 없이 새 시간표를 제공한다. 앱은 마지막으로 받은 시간표를 로컬에 보관하고, 앱 시작 시 HTTP 조건부 요청으로 변경 여부만 확인한다.

초기 구현은 현재 앱이 사용하는 명지대역 평일·주말 시간표와 기흥역 평일 시간표를 하나의 불변 릴리스로 발행한다. 관리자 화면, 시간표 행별 편집 API, 예약 발행은 포함하지 않는다.

## 2. 확정 설계

| 항목 | 결정 |
| --- | --- |
| 원본 저장소 | DB의 `timetable_release` 테이블 |
| 버전 | 릴리스 행의 증가하는 `BIGINT` 기본 키 |
| 시간표 저장 형식 | 릴리스 전체를 담은 JSON 문자열 |
| 현재 버전 | `version`이 가장 큰 릴리스 |
| 수정 방식 | 기존 릴리스를 수정하지 않고 새 행 삽입 |
| 공개 API | `GET /timetables/current` 하나 |
| 변경 확인 | `ETag`와 `If-None-Match` |
| 인증 | 공개 조회이므로 불필요 |
| 서버 캐시 | 초기 구현에서는 사용하지 않음 |
| 발행 방식 | Flyway 마이그레이션으로 검증된 새 릴리스 삽입 |

시간표 데이터는 작고 서버에서 조건 검색하지 않으므로 노선·정류장·운행편 테이블로 나누지 않는다. 검색이나 부분 수정 요구가 실제로 생기기 전까지 릴리스 JSON 하나가 원자성과 롤백을 가장 단순하게 보장한다.

## 3. API 계약

### 3.1 현재 시간표 조회

```http
GET /timetables/current
If-None-Match: "7"
```

클라이언트 버전이 다르거나 헤더가 없으면 `200 OK`를 반환한다.

```http
HTTP/1.1 200 OK
ETag: "8"
Cache-Control: no-cache
Content-Type: application/json
```

```json
{
  "status": 200,
  "data": {
    "version": 8,
    "timetable": {
      "myongjiWeekday": [
        {
          "id": 0,
          "type": "명지대역",
          "startTime": "8:00",
          "predTime": "8:15"
        }
      ],
      "myongjiWeekend": [
        {
          "id": 0,
          "startTime": "8:20",
          "predTime": "8:45"
        }
      ],
      "giheungWeekday": [
        {
          "id": 0,
          "startTime": "8:00",
          "predTime": "8:15",
          "schoolArrival": "8:30",
          "runCount": 2
        }
      ]
    }
  }
}
```

클라이언트의 `If-None-Match`가 현재 릴리스와 같으면 본문 없이 반환한다.

```http
HTTP/1.1 304 Not Modified
ETag: "8"
Cache-Control: no-cache
```

`ETag`는 릴리스 버전을 큰따옴표로 감싼 강한 태그다. 릴리스는 생성 후 변경하지 않으므로 같은 태그는 항상 같은 응답 본문을 의미한다.

### 3.2 실패 계약

- 릴리스가 하나도 없으면 `503 Service Unavailable`을 반환한다.
- 저장된 JSON을 역직렬화할 수 없으면 `500 Internal Server Error`로 기록하고 잘못된 데이터를 반환하지 않는다.
- 오류 본문은 기존 `YongJiResponse` 형식을 유지한다.
- `304`에는 `YongJiResponse` 본문을 넣지 않는다.

## 4. 데이터 모델

```sql
CREATE TABLE timetable_release (
    version BIGINT AUTO_INCREMENT PRIMARY KEY,
    payload LONGTEXT NOT NULL,
    published_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
```

`LONGTEXT`를 사용하는 이유는 현재 운영 MySQL과 테스트 H2의 타입 차이를 최소화하기 위해서다. 애플리케이션이 Jackson으로 JSON을 DTO에 역직렬화하고 검증한다.

### 4.1 payload 규칙

- 최상위 키는 `myongjiWeekday`, `myongjiWeekend`, `giheungWeekday` 세 개다.
- 배열 순서가 앱의 표시 순서다.
- `id`는 각 배열 안에서 중복되지 않는 0 이상의 정수다.
- 시각은 현재 데이터와 호환되는 `H:mm` 또는 `HH:mm` 형식이며 유효한 24시간 시각이어야 한다.
- `type`, `startTime`, `predTime`, `schoolArrival` 문자열은 앞뒤 공백이 없어야 한다.
- `runCount`는 1 이상의 정수다.
- 빈 배열은 운행 없음이라는 유효한 상태로 허용한다.

## 5. 패키지와 책임

```text
com.yongjibus.timetable/
├── controller/TimetableController.java
├── controller/dto/TimetableReleaseResponseDTO.java
├── controller/dto/TimetablePayloadDTO.java
├── domain/TimetableRelease.java
├── repository/TimetableReleaseRepository.java
└── service/TimetableService.java
```

- Repository: 최신 릴리스 한 건 조회
- Service: JSON 역직렬화와 데이터 검증
- Controller: 공개 조회, `ETag`, `Cache-Control`, `304` 처리
- Flyway: 테이블 생성과 최초 릴리스 삽입

DTO는 화면에 필요한 필드만 가진 중첩 `record`로 구현한다. 단일 구현을 위한 별도 인터페이스나 매퍼 계층은 만들지 않는다.

## 6. 릴리스 운영 규칙

1. 기존 앱 JSON 세 개를 영문 필드 DTO 형식으로 변환한다.
2. 새 Flyway 마이그레이션에서 `timetable_release`에 한 행을 삽입한다.
3. 백엔드 테스트로 JSON 역직렬화와 규칙 검증을 통과시킨다.
4. 백엔드를 배포하고 `200`, `ETag`, 조건부 `304`를 확인한다.
5. 잘못 발행했을 때 기존 행을 수정하지 않고 올바른 payload를 새 행으로 다시 발행한다.

버전 값을 사람이 직접 재사용하지 않는다. 이전 시간표로 되돌리는 경우에도 이전 payload를 새 릴리스로 삽입해 버전은 계속 증가시킨다.

## 7. 구현 체크리스트

### DB와 초기 데이터

- [x] `V6__add_timetable_release.sql`에 `timetable_release` 테이블을 추가한다.
- [x] 같은 마이그레이션 또는 다음 마이그레이션에 현재 시간표 릴리스 한 건을 삽입한다.
- [x] 기존 세 JSON의 모든 행이 누락 없이 변환됐는지 개수와 주요 첫·마지막 행을 대조한다.
- [x] `FlywayMigrationTest`의 최신 버전 기대값을 새 마이그레이션 버전으로 변경한다.
- [x] 빈 DB와 기존 DB 양쪽에서 마이그레이션 테스트가 통과하는지 확인한다.

### 도메인과 저장소

- [x] `TimetableRelease` 엔티티에 `version`, `payload`, `publishedAt`만 둔다.
- [x] 릴리스 수정·삭제 메서드는 만들지 않는다.
- [x] Repository에 최신 버전 한 건 조회만 추가한다.
- [x] 릴리스가 없을 때 명확한 서버 오류로 변환한다.

### DTO와 검증

- [x] API 필드를 `id`, `type`, `startTime`, `predTime`, `schoolArrival`, `runCount`로 통일한다.
- [x] 세 배열의 존재 여부를 검증한다.
- [x] 각 배열의 ID 중복과 음수를 거부한다.
- [x] 모든 시각 문자열을 실제 `LocalTime`으로 파싱해 검증한다.
- [x] 필수 문자열의 `null`, 빈 값, 앞뒤 공백을 거부한다.
- [x] 기흥역 `runCount < 1`을 거부한다.
- [x] 잘못된 릴리스는 응답으로 내보내지 않고 서버 로그에 버전을 남긴다.

### 서비스와 컨트롤러

- [x] 최신 릴리스 조회와 payload 역직렬화를 서비스에 구현한다.
- [x] `GET /timetables/current`를 추가한다.
- [x] `200` 응답에 `ETag: "{version}"`을 포함한다.
- [x] `Cache-Control: no-cache`를 포함한다.
- [x] Spring의 조건부 요청 처리를 사용해 일치하는 `If-None-Match`에 `304`를 반환한다.
- [x] `304` 응답 본문이 비어 있는지 확인한다.
- [x] 별도 `/version` 엔드포인트는 만들지 않는다.

### 보안과 CORS

- [x] `GET /timetables/**`를 인증 없이 허용한다.
- [x] 쓰기 API는 만들지 않는다.
- [x] 웹 앱에서도 `ETag`를 읽도록 CORS 노출 헤더에 `ETag`를 추가한다.
- [x] 공개 API에 사용자 정보나 인증 토큰이 필요하지 않은지 확인한다.

### 테스트

- [x] Service 테스트: 최신 버전과 역직렬화된 시간표를 반환한다.
- [x] Service 테스트: 릴리스 없음과 잘못된 JSON을 거부한다.
- [x] Service 테스트: 중복 ID, 잘못된 시각, 잘못된 `runCount`를 거부한다.
- [x] Controller 테스트: 헤더가 없으면 `200`, 본문, `ETag`를 반환한다.
- [x] Controller 테스트: 다른 태그이면 새 `200` 응답을 반환한다.
- [x] Controller 테스트: 같은 태그이면 빈 `304`를 반환한다.
- [x] Security 테스트: 비로그인 요청이 허용된다.
- [x] 초기 릴리스 payload 전체가 DTO 검증을 통과하는 테스트를 둔다.

### 배포 확인

- [ ] 백엔드를 앱보다 먼저 배포한다.
- [ ] 운영 DB에 최초 릴리스가 한 건 존재하는지 확인한다.
- [ ] `curl -i /timetables/current`로 `ETag`와 본문을 확인한다.
- [ ] 받은 태그를 `If-None-Match`로 다시 보내 `304`를 확인한다.
- [ ] 새 릴리스 삽입 후 이전 태그 요청이 새 버전 `200`을 받는지 확인한다.
- [ ] API 실패율과 payload 역직렬화 오류 로그를 배포 직후 확인한다.

## 8. 완료 조건

- 앱이 로그인 없이 현재 시간표를 받을 수 있다.
- 동일 버전 재요청은 본문 없는 `304`를 반환한다.
- 새 릴리스 삽입만으로 앱 재배포 없이 시간표가 갱신된다.
- 잘못된 릴리스가 조용히 앱 캐시를 덮어쓰지 않는다.
- 과거 릴리스는 유지되며 롤백도 새 버전 발행으로 처리된다.

## 9. 초기 범위에서 제외

- 관리자 시간표 편집 화면과 쓰기 API
- 예약 발행과 유효 기간
- 행 단위 CRUD와 관계형 정규화
- Redis·Caffeine·CDN 캐시
- 버전별 diff 응답

운영자가 배포 없이 시간표를 자주 수정해야 할 때만 `ROLE_OPERATOR` 기반 발행 API를 추가한다.
