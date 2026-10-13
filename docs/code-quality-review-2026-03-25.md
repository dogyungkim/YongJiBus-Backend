# YongJiBus Backend Code Quality Review

- Review date: 2026-03-25
- Project: `YongJiBus-Backend`
- Basis: user-provided code quality, testing, architecture, and review standards

## Review Summary

이번 리뷰에서는 `auth/global`, `chat/websocket`, `arrivaltime/daytype/vacation/scheduler` 영역을 함께 검토했다.  
중요도 높은 이슈는 크게 네 가지 축으로 모였다.

1. 인증 우회 가능성
2. 데이터 무결성 훼손 가능성
3. 공개되면 안 되는 쓰기 엔드포인트 노출
4. 캐시/스케줄링에 따른 상태 불일치

## Verification Notes

- 전체 테스트 실행: `./gradlew test`
- 결과: 실패
- 직접 확인된 실패 테스트: `src/test/java/com/yongjibus/daytype/HolidayApiClientTest.java:56`
- 실패 원인: 테스트는 raw `serviceKey`를 기대하지만 실제 클라이언트는 URL encoding 된 값을 전송함
- 정적 분석 도구:
  - `clippy`, `miri`는 이 프로젝트(Java/Spring)와 직접적으로 맞지 않음
  - Java용 SpotBugs, Checkstyle, PMD, ErrorProne 등은 `build.gradle`에 설정되어 있지 않음

## High Severity Findings

### 1. Refresh token이 access token처럼 허용됨

- 영향: refresh token만 있으면 일반 REST API와 STOMP 인증까지 가능
- 위치:
  - `src/main/java/com/yongjibus/global/infra/jwt/JwtService.java:100`
  - `src/main/java/com/yongjibus/global/config/security/JwtAuthenticationProcessingFilter.java:79`
  - `src/main/java/com/yongjibus/global/infra/websocket/StompPreHandler.java:72`
- 상세:
  - `validateAccessToken()`이 만료/서명만 확인하고 토큰 subject가 `AccessToken`인지 확인하지 않음
  - 결과적으로 7일짜리 refresh token이 보호 API 접근 토큰으로 재사용될 수 있음
- 권장 조치:
  - access/refresh token에 대해 subject 검증을 분리
  - 필터와 STOMP pre-handler에서 access token 타입을 명시적으로 확인
  - 관련 회귀 테스트 추가

### 2. 비인증 사용자가 운영 데이터를 수정할 수 있음

- 영향: 누구나 도착 시간 저장 및 방학 기간 변경 가능
- 위치:
  - `src/main/java/com/yongjibus/global/config/security/SecurityConfig.java:45`
  - `src/main/java/com/yongjibus/arrivaltime/controller/ArrivalTimeController.java:34`
  - `src/main/java/com/yongjibus/vacation/controller/VacationController.java:34`
- 상세:
  - `/arrivaltime/**`, `/vacation/**`가 `permitAll`
  - 쓰기 엔드포인트인 `/arrivaltime/save`, `/vacation/vacation-period`도 보호되지 않음
- 권장 조치:
  - 읽기/쓰기 권한 분리
  - 관리자 전용 경로 또는 인증 기반 접근 제어 추가
  - 보안 테스트 추가

### 3. 채팅방 정원 검사가 동시성에 안전하지 않음

- 영향: 동시 요청 시 정원 초과 입장 가능, 이미 입장한 사용자의 재시도도 오작동
- 위치:
  - `src/main/java/com/yongjibus/chat/service/ChatService.java:83`
  - `src/main/java/com/yongjibus/chat/service/ChatService.java:88`
  - `src/main/java/com/yongjibus/chat/service/ChatService.java:93`
  - `src/main/java/com/yongjibus/chat/repository/ChatRoomRepository.java:7`
- 상세:
  - 정원 체크가 active membership 확인보다 먼저 수행됨
  - DB lock, optimistic lock, unique guard 중 어느 것도 없음
  - 동시 join으로 5명을 초과할 수 있고, 이미 참여 중인 사용자의 재호출도 `CHAT_ROOM_FULL`로 실패 가능
- 권장 조치:
  - 멤버 여부 확인을 정원 체크보다 먼저 수행
  - row-level lock 또는 version 기반 락 도입
  - 동시성 테스트 추가

### 4. dayInfo 캐시가 잘못된 휴일/방학 상태를 하루 동안 유지할 수 있음

- 영향: 특정 날짜가 실제와 다르게 평일/휴일/방학으로 응답될 수 있음
- 위치:
  - `src/main/java/com/yongjibus/daytype/service/DayTypeService.java:43`
  - `src/main/java/com/yongjibus/global/config/cache/CacheConfig.java:26`
  - `src/main/java/com/yongjibus/scheduler/CalendarScheduledTask.java:17`
  - `src/main/java/com/yongjibus/scheduler/HolidayScheduledTask.java:28`
  - `src/main/java/com/yongjibus/vacation/service/VacationService.java:24`
- 상세:
  - `dayInfo` 캐시 TTL이 24시간
  - 월초 00:00에 저장소를 평일/주말 기준으로 초기화하고, 공휴일 적재는 06:00에 수행
  - startup 초기 로딩은 비활성화
  - 방학 기간 저장 후에도 cache eviction이 없음
- 권장 조치:
  - 휴일/방학 데이터 변경 시 `dayInfo` 캐시 무효화
  - 초기 적재와 스케줄 갱신 순서 재설계
  - 월초/방학 저장 직후 회귀 테스트 추가

### 5. 이메일 유니크 제약이 애플리케이션 레벨에만 존재함

- 영향: 동시 회원가입 시 동일 이메일 중복 저장 가능
- 위치:
  - `src/main/java/com/yongjibus/member/domain/Member.java:41`
  - `src/main/java/com/yongjibus/member/service/MemberService.java:30`
  - `src/main/java/com/yongjibus/member/repository/MemberRepository.java:10`
  - `database_schema.sql:17`
- 상세:
  - 코드에서는 `existsByEmail()`로만 사전 확인
  - 엔티티와 스키마에 email unique 제약이 없음
  - 중복 데이터가 생기면 `findByEmail()` 기반 인증 흐름이 모호해짐
- 권장 조치:
  - DB unique constraint 추가
  - 중복 예외를 일관된 도메인 에러로 변환
  - 동시 회원가입 테스트 추가

## Medium Severity Findings

### 6. 채팅 메시지 외부 전송이 트랜잭션 커밋 전에 발생함

- 영향: DB 롤백 후에도 WebSocket/FCM에는 메시지가 전달될 수 있음
- 위치:
  - `src/main/java/com/yongjibus/chat/service/ChatService.java:167`
  - `src/main/java/com/yongjibus/chat/service/ChatService.java:185`
  - `src/main/java/com/yongjibus/chat/service/ChatService.java:192`
- 상세:
  - 저장 후 같은 트랜잭션 안에서 즉시 FCM 및 STOMP 발송
  - 이후 flush/commit 실패 시 사용자 관점과 DB 상태가 불일치
- 권장 조치:
  - 트랜잭션 after-commit 이벤트로 발행 이동
  - 메시지 저장과 전송 책임 분리

### 7. 같은 디바이스 토큰으로 재로그인 시 FCM 토큰 등록이 깨질 수 있음

- 영향: 로그아웃 후 같은 토큰 재등록 시 unique key 충돌로 500 가능
- 위치:
  - `src/main/java/com/yongjibus/chat/service/FCMTokenService.java:22`
  - `src/main/java/com/yongjibus/chat/controller/ChatController.java:231`
  - `database_schema.sql:87`
- 상세:
  - 로그아웃은 row를 삭제하지 않고 비활성화만 함
  - 재등록은 inactive row를 재활성화하지 않고 새 insert를 시도
  - 스키마는 `(member_id, token)` unique
- 권장 조치:
  - inactive token 재활성화 경로 추가
  - 동일 token 재등록 테스트 추가

### 8. 회원 탈퇴 후 기존 access token이 장시간 유효함

- 영향: 탈퇴 직후에도 최대 10시간 보호 API 접근 가능
- 위치:
  - `src/main/java/com/yongjibus/auth/service/AuthService.java:167`
  - `src/main/java/com/yongjibus/auth/service/MemberDetailService.java:21`
  - `src/main/resources/application-prod.yml:57`
- 상세:
  - 탈퇴 시 refresh token만 삭제
  - `MemberDetailService`가 `isDeleted` 회원을 차단하지 않음
  - access token validity가 10시간
- 권장 조치:
  - 인증 시 `isDeleted` 검사 추가
  - access token blacklist 또는 token versioning 검토

### 9. 방학 기간 데이터의 불변식 검증이 없음

- 영향: 역전된 기간 저장, 중복 기간 저장, 조회 시 500 발생 가능
- 위치:
  - `src/main/java/com/yongjibus/vacation/domain/VacationForm.java:12`
  - `src/main/java/com/yongjibus/vacation/controller/VacationController.java:42`
  - `src/main/java/com/yongjibus/vacation/repository/VacationPeriodRepository.java:9`
  - `src/main/java/com/yongjibus/vacation/service/VacationService.java:42`
- 상세:
  - `startDate <= endDate` 검증 없음
  - 겹치는 기간 차단 없음
  - 단건을 가정하는 `findByStartDateLessThanEqualAndEndDateGreaterThanEqual()`가 다건 매칭 시 깨질 수 있음
- 권장 조치:
  - 입력 검증 추가
  - 기간 중복 방지 규칙 정의
  - 저장/조회 테스트 보강

### 10. 인증/이메일 검증 상태가 전부 인메모리 저장소에만 있음

- 영향: 서버 재시작 또는 멀티 인스턴스 배포 시 인증 일관성 붕괴
- 위치:
  - `src/main/java/com/yongjibus/auth/service/InMemoryJwtRepository.java:10`
  - `src/main/java/com/yongjibus/auth/email/InMemoryEmailTokenService.java:11`
  - `src/main/java/com/yongjibus/auth/email/InMemoryEmailPendingRepository.java:8`
- 상세:
  - refresh token, email verification, bounce pending 상태가 프로세스 로컬 메모리에만 존재
  - 재시작 시 로그아웃/인증 상태 소실
- 권장 조치:
  - Redis 등 외부 저장소로 이전
  - 다중 인스턴스 환경 기준 테스트 추가

### 11. 이메일 전송 실패 시 인증 코드 상태가 어긋날 수 있음

- 영향: 메일을 못 받은 사용자에게 유효한 auth code 상태가 남을 수 있음
- 위치:
  - `src/main/java/com/yongjibus/auth/service/AuthService.java:37`
  - `src/main/java/com/yongjibus/global/infra/email/EmailService.java:29`
- 상세:
  - auth code를 먼저 캐시에 저장하고 메일 전송 수행
  - `EmailService`는 `MessagingException`만 처리
  - 실제 `JavaMailSender.send()` 실패는 `MailException` 계열인 경우가 많음
- 권장 조치:
  - 저장과 발송 순서 재검토
  - 전송 실패 예외 처리 범위 확장
  - 실패 시 캐시 rollback 또는 무효화

### 12. Gmail bounce 처리 로직이 history cursor를 잘못 사용함

- 영향: 실제 bounce 메일을 놓칠 수 있고 재시작 후 복구 불가
- 위치:
  - `src/main/java/com/yongjibus/auth/email/GmailBounceController.java:50`
  - `src/main/java/com/yongjibus/auth/email/EmailBounceService.java:26`
  - `src/main/java/com/yongjibus/global/infra/gmail/GmailApiService.java:46`
- 상세:
  - Pub/Sub notification의 `historyId`를 그대로 `startHistoryId`로 사용
  - 이전 checkpoint를 저장하지 않음
  - Gmail history는 supplied ID 이후만 반환하므로 triggering event 자체를 놓칠 수 있음
- 권장 조치:
  - last processed history id 저장
  - startup recovery flow 추가
  - integration-style 테스트 추가

### 13. WebSocket presence 추적이 멀티 세션을 지원하지 않음

- 영향: 한 사용자가 여러 세션을 열면 일부 세션이 남아 있어도 오프라인으로 간주되어 불필요한 푸시 전송
- 위치:
  - `src/main/java/com/yongjibus/global/infra/websocket/InMemoryWebsocketSessionManager.java:13`
  - `src/main/java/com/yongjibus/chat/service/ChatService.java:181`
- 상세:
  - email당 sessionId 하나만 저장
  - 저장된 세션 하나가 끊기면 다른 활성 세션 존재 여부를 표현할 수 없음
- 권장 조치:
  - email -> session set 구조로 변경
  - 멀티 세션 테스트 추가

### 14. Request validation과 에러 매핑이 일관되지 않음

- 영향: 잘못된 입력이 도메인 에러 대신 generic 400/500으로 새어 나감
- 위치:
  - `src/main/java/com/yongjibus/global/error/handler/GlobalExceptionHandler.java:27`
  - `src/main/java/com/yongjibus/daytype/controller/DayTypeController.java:57`
  - `src/main/java/com/yongjibus/arrivaltime/controller/ArrivalTimeController.java:42`
  - `src/main/java/com/yongjibus/chat/controller/ChatController.java:117`
  - `src/main/java/com/yongjibus/chat/controller/ChatController.java:208`
  - `src/main/java/com/yongjibus/arrivaltime/controller/dto/SaveArrivalTimeRequestDTO.java:6`
- 상세:
  - `MethodArgumentNotValidException`를 전부 `INVALID_DATE_FORMAT`으로 처리
  - `LocalDate` 변환 실패는 별도 예외로 빠져 `YongJiResponse`가 보장되지 않음
  - 일부 DTO는 제약 조건이 없고 `@Valid`도 없음
- 권장 조치:
  - validation error code 분리
  - type mismatch 예외 핸들러 추가
  - DTO 제약과 controller `@Valid` 보강

### 15. 신고 저장과 메일 발송이 원자적으로 묶여 있지 않음

- 영향: 클라이언트는 실패를 받았는데 신고는 DB에 남는 상태 가능
- 위치:
  - `src/main/java/com/yongjibus/member/service/ReportService.java:25`
  - `src/main/java/com/yongjibus/global/infra/email/EmailService.java:48`
- 상세:
  - 신고 저장 후 메일 전송
  - 메일 실패 시 이미 저장된 레코드와 응답 결과가 어긋날 수 있음
- 권장 조치:
  - 트랜잭션/비동기 이벤트 분리 전략 수립
  - 클라이언트 응답 의미 재정의

### 16. 도착 시간 저장/조회 모델이 중복과 순서 비결정성을 허용함

- 영향: 재시도 시 중복 데이터가 쌓이고, 조회 순서가 DB 구현에 좌우됨
- 위치:
  - `src/main/java/com/yongjibus/arrivaltime/service/ArrivalTimeService.java:32`
  - `src/main/java/com/yongjibus/arrivaltime/repository/ArrivalTimeRepository.java:11`
  - `database_schema.sql:31`
- 상세:
  - natural key uniqueness 없음
  - read query에 `ORDER BY`가 일관되게 강제되지 않음
- 권장 조치:
  - `(time_id, date, time, is_holiday)` 등 비즈니스 키 검토
  - 정렬 기준을 repository 레벨에서 명시

## Low Severity Findings

### 17. `/gmail/bounce/**`가 공개되어 있고 호출자 검증이 없음

- 영향: 외부에서 히스토리 스캔 유발 가능, bounce 상태 노출 가능
- 위치:
  - `src/main/java/com/yongjibus/global/config/security/SecurityConfig.java:43`
  - `src/main/java/com/yongjibus/auth/email/GmailBounceController.java:34`
  - `src/main/java/com/yongjibus/auth/email/GmailBounceController.java:74`
- 권장 조치:
  - Pub/Sub 검증 헤더 또는 서명 검증 추가
  - GET 조회 엔드포인트 재검토

### 18. Holiday API 테스트가 현재 false negative 상태임

- 영향: 실제 코드가 맞아도 테스트가 실패하여 신뢰도 저하
- 위치:
  - `src/test/java/com/yongjibus/daytype/HolidayApiClientTest.java:56`
  - `src/test/java/com/yongjibus/daytype/HolidayApiClientTest.java:58`
  - `src/main/java/com/yongjibus/daytype/client/HolidayApiClient.java:30`
- 상세:
  - 테스트는 raw `serviceKey`를 기대
  - 실제 구현은 URL encoding 수행
- 권장 조치:
  - encoded URL 기준으로 expectation 수정

## Test Coverage Gaps

- refresh token이 access token처럼 사용되는 회귀를 잡는 테스트 없음
- 채팅방 동시 join/정원 초과 race 테스트 없음
- FCM inactive token 재등록 테스트 없음
- dayInfo cache eviction/월초 스케줄 간극 테스트 없음
- 방학 기간 역전/중복 입력 검증 테스트 없음
- Gmail history cursor 처리 테스트 없음
- 멀티 웹소켓 세션 presence 테스트 없음

## Suggested Fix Order

1. 인증 토큰 타입 검증 수정
2. 공개 쓰기 엔드포인트 보호
3. 이메일 unique constraint 추가
4. 채팅방 join 동시성 제어
5. dayInfo 캐시 무효화 전략 도입
6. 방학 기간 불변식 검증
7. 인메모리 인증 상태 외부 저장소 이전
8. 테스트 및 정적 분석 체계 보강
