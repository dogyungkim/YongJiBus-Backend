# YongJiBus Backend Test Coverage Review

- Review date: 2026-03-30
- Project: `YongJiBus-Backend`
- Scope: 현재 코드베이스의 도메인별 테스트 충분성 점검
- Basis:
  - `src/main/java/com/yongjibus/**`
  - `src/test/java/com/yongjibus/**`
  - 도메인별 병렬 서브 에이전트 리뷰 결과 통합

## Overall Verdict

- 전체 판정: `부분 충분`

현재 프로젝트는 서비스 레벨 회귀 테스트가 많이 보강되어 있고, 최근 수정한 위험한 버그들도 꽤 잘 고정되어 있다.  
반면 `controller`, `security/filter`, `scheduler`, 일부 외부 연동 경계 테스트는 아직 얇다.  
즉, 비즈니스 로직 회귀에는 강하지만 실제 요청 흐름과 프레임워크 wiring 회귀에는 아직 취약한 구조다.

## Domain Summary

| Domain | Verdict | Notes |
| --- | --- | --- |
| `auth` | 부분 충분 | 서비스/토큰/bounce cursor는 강함, 컨트롤러와 HTTP security 경계는 약함 |
| `global` | 부분 충분 | JWT, 예외 응답, 이메일 유틸은 좋음, 필터/보안 설정/STOMP error handler는 비어 있음 |
| `chat/websocket` | 부분 충분 | 서비스/동시성/after-commit/presence는 강함, controller/쿼리/푸시 경계는 부족 |
| `arrivaltime` | 부족 | 기본 저장/조회만 있음, 핵심 조회 규약과 controller 경계가 약함 |
| `daytype` | 부분 충분 | 서비스/캐시/API client는 강함, controller/startup lifecycle은 약함 |
| `vacation` | 부분 충분 | 불변식과 repository는 강함, MVC 폼/화면 응답은 약함 |
| `member/report` | 부분 충분 | 신고 after-commit 흐름은 탄탄, controller/request validation은 약함 |
| `scheduler` | 부족 | Gmail watch만 테스트, 나머지 스케줄러는 비어 있음 |

## Domain Details

### 1. Auth

- 판정: `부분 충분`
- 잘 커버된 영역:
  - 이메일 발송 후 상태 저장 순서, 실패 시 cleanup, 로그인/회원가입/토큰 재발급
  - 삭제 회원 차단
  - Gmail bounce history checkpoint 처리
- 대표 테스트:
  - [AuthServiceTest.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/test/java/com/yongjibus/auth/service/AuthServiceTest.java)
  - [MemberDetailServiceTest.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/test/java/com/yongjibus/auth/service/MemberDetailServiceTest.java)
  - [EmailBounceServiceTest.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/test/java/com/yongjibus/auth/email/EmailBounceServiceTest.java)
- 부족한 영역:
  - [AuthController.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/main/java/com/yongjibus/auth/controller/AuthController.java) 컨트롤러 테스트 없음
  - [GmailBounceController.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/main/java/com/yongjibus/auth/email/GmailBounceController.java) Pub/Sub payload parsing 경계 테스트 없음
  - [InMemoryJwtRepository.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/main/java/com/yongjibus/auth/service/InMemoryJwtRepository.java), [InMemoryEmailTokenService.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/main/java/com/yongjibus/auth/email/InMemoryEmailTokenService.java) 구현 단위 테스트 없음

### 2. Global

- 판정: `부분 충분`
- 잘 커버된 영역:
  - access/refresh token 타입 검증
  - 공통 에러 응답 포맷
  - 메일 전송 예외 매핑
  - Gmail API 일부 유틸
- 대표 테스트:
  - [JwtServiceTest.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/test/java/com/yongjibus/global/jwt/JwtServiceTest.java)
  - [GlobalExceptionHandlerTest.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/test/java/com/yongjibus/global/error/handler/GlobalExceptionHandlerTest.java)
  - [EmailServiceTest.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/test/java/com/yongjibus/global/infra/email/EmailServiceTest.java)
  - [GmailApiServiceTest.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/test/java/com/yongjibus/global/infra/gmail/GmailApiServiceTest.java)
- 부족한 영역:
  - [JwtAuthenticationProcessingFilter.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/main/java/com/yongjibus/global/config/security/JwtAuthenticationProcessingFilter.java) 필터 체인 테스트 없음
  - [SecurityConfig.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/main/java/com/yongjibus/global/config/security/SecurityConfig.java) 실제 보호 경로 테스트 없음
  - [WebsocketStompErrorHandler.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/main/java/com/yongjibus/global/error/handler/WebsocketStompErrorHandler.java) STOMP error frame 테스트 없음
  - [MjuEmailValidator.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/main/java/com/yongjibus/global/infra/email/MjuEmailValidator.java) 단위 테스트 없음

### 3. Chat / Websocket

- 판정: `부분 충분`
- 잘 커버된 영역:
  - 채팅방 생성/입장/메시지 저장/배달 위임
  - after-commit 배달 보장
  - FCM 토큰 저장/재활성화
  - 채팅방 동시 참여 정원 제어
  - 멀티 세션 presence 유지
  - STOMP CONNECT/SUBSCRIBE 인증/인가
- 대표 테스트:
  - [ChatServiceTest.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/test/java/com/yongjibus/chat/service/ChatServiceTest.java)
  - [ChatServiceConcurrencyTest.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/test/java/com/yongjibus/chat/service/ChatServiceConcurrencyTest.java)
  - [ChatMessageDeliveryPublisherTest.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/test/java/com/yongjibus/chat/service/ChatMessageDeliveryPublisherTest.java)
  - [ChatMessageDeliveryServiceTest.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/test/java/com/yongjibus/chat/service/ChatMessageDeliveryServiceTest.java)
  - [ChatMessageDeliveryServiceTransactionalTest.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/test/java/com/yongjibus/chat/service/ChatMessageDeliveryServiceTransactionalTest.java)
  - [FCMTokenServiceTest.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/test/java/com/yongjibus/chat/service/FCMTokenServiceTest.java)
  - [StompPreHandlerTest.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/test/java/com/yongjibus/global/infra/websocket/StompPreHandlerTest.java)
  - [InMemoryWebsocketSessionManagerTest.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/test/java/com/yongjibus/global/infra/websocket/InMemoryWebsocketSessionManagerTest.java)
- 부족한 영역:
  - [ChatController.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/main/java/com/yongjibus/chat/controller/ChatController.java) REST 경계 테스트 없음
  - [FCMNotificationService.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/main/java/com/yongjibus/chat/service/FCMNotificationService.java) 실제 payload/예외 처리 테스트 없음
  - [ChatService.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/main/java/com/yongjibus/chat/service/ChatService.java) `leaveChatRoom` 세부 분기 테스트 부족
  - [ChatRepository.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/main/java/com/yongjibus/chat/repository/ChatRepository.java), [ChatRoomMemberRepository.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/main/java/com/yongjibus/chat/repository/ChatRoomMemberRepository.java) 조회 쿼리 회귀 테스트 부족
  - [StompPreHandler.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/main/java/com/yongjibus/global/infra/websocket/StompPreHandler.java) `SEND`/`DISCONNECT` 분기 직접 테스트 부족

### 4. Arrivaltime

- 판정: `부족`
- 잘 커버된 영역:
  - 기본 저장
  - 단일 조회
  - 날짜별 그룹 조회
- 대표 테스트:
  - [ArrivalTimeRepositoryTest.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/test/java/com/yongjibus/arrivaltime/ArrivalTimeRepositoryTest.java)
  - [ArrivalTimeServiceTest.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/test/java/com/yongjibus/arrivaltime/ArrivalTimeServiceTest.java)
- 부족한 영역:
  - [ArrivalTimeService.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/main/java/com/yongjibus/arrivaltime/service/ArrivalTimeService.java) 최근 5개 조회 경로 직접 테스트 부족
  - 정렬 규약과 빈 결과 예외 분기 부족
  - [ArrivalTimeController.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/main/java/com/yongjibus/arrivaltime/controller/ArrivalTimeController.java) 성공/검증/에러 응답 테스트 없음
  - [ArrivalTimeRepository.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/main/java/com/yongjibus/arrivaltime/repository/ArrivalTimeRepository.java) 정렬/중복 회귀 테스트 부족

### 5. Daytype

- 판정: `부분 충분`
- 잘 커버된 영역:
  - 기본 조회
  - 공휴일 API 적재
  - 캐시 무효화
  - Holiday API URL 구성
  - 인메모리 저장소 초기화
- 대표 테스트:
  - [DayTypeServiceTest.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/test/java/com/yongjibus/daytype/DayTypeServiceTest.java)
  - [DayTypeServiceCachingTest.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/test/java/com/yongjibus/daytype/DayTypeServiceCachingTest.java)
  - [HolidayApiClientTest.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/test/java/com/yongjibus/daytype/HolidayApiClientTest.java)
  - [InMemoryDayTypeRepositoryTest.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/test/java/com/yongjibus/daytype/InMemoryDayTypeRepositoryTest.java)
- 부족한 영역:
  - [DayTypeController.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/main/java/com/yongjibus/daytype/controller/DayTypeController.java) 성공/404 응답 테스트 없음
  - [DayTypeService.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/main/java/com/yongjibus/daytype/service/DayTypeService.java) startup refresh 동작 테스트 부족
  - malformed XML 파싱 실패를 직접 고정하는 테스트 약함

### 6. Vacation

- 판정: `부분 충분`
- 잘 커버된 영역:
  - 역전 기간 차단
  - 중복 기간 차단
  - 경계 날짜 포함 여부
  - repository 범위 조회
- 대표 테스트:
  - [VacationServiceTest.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/test/java/com/yongjibus/vacation/service/VacationServiceTest.java)
  - [VacationPeriodRepositoryTest.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/test/java/com/yongjibus/vacation/repository/VacationPeriodRepositoryTest.java)
- 부족한 영역:
  - [VacationController.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/main/java/com/yongjibus/vacation/controller/VacationController.java) MVC 흐름 테스트 없음
  - [VacationForm.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/main/java/com/yongjibus/vacation/domain/VacationForm.java) binding/validation 분기 직접 테스트 없음
  - 성공 메시지 및 화면 model 테스트 없음

### 7. Member / Report

- 판정: `부분 충분`
- 잘 커버된 영역:
  - 이메일 중복 저장 차단
  - 신고 저장 후 after-commit 메일 발행
  - 메일 실패가 신고 성공 의미를 깨지 않음
  - rollback 시 메일 미전송
- 대표 테스트:
  - [MemberServiceTest.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/test/java/com/yongjibus/member/service/MemberServiceTest.java)
  - [MemberRepositoryTest.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/test/java/com/yongjibus/auth/repository/MemberRepositoryTest.java)
  - [ReportServiceTest.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/test/java/com/yongjibus/member/service/ReportServiceTest.java)
  - [ReportServiceTransactionalTest.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/test/java/com/yongjibus/member/service/ReportServiceTransactionalTest.java)
  - [ReportEmailPublisherTest.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/test/java/com/yongjibus/member/service/ReportEmailPublisherTest.java)
  - [ReportEmailDeliveryServiceTest.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/test/java/com/yongjibus/member/service/ReportEmailDeliveryServiceTest.java)
  - [ReportEmailDeliveryServiceTransactionalTest.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/test/java/com/yongjibus/member/service/ReportEmailDeliveryServiceTransactionalTest.java)
- 부족한 영역:
  - [MemberController.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/main/java/com/yongjibus/member/controller/MemberController.java) 테스트 없음
  - [ReportController.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/main/java/com/yongjibus/member/controller/ReportController.java) request validation/auth wiring 테스트 없음
  - [MemberReport.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/main/java/com/yongjibus/member/domain/MemberReport.java) persistence/repository 테스트 없음

### 8. Scheduler

- 판정: `부족`
- 잘 커버된 영역:
  - Gmail watch 등록 시 초기 checkpoint 저장
- 대표 테스트:
  - [GmailWatchScheduledTaskTest.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/test/java/com/yongjibus/scheduler/GmailWatchScheduledTaskTest.java)
- 부족한 영역:
  - [CalendarScheduledTask.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/main/java/com/yongjibus/scheduler/CalendarScheduledTask.java) 테스트 없음
  - [HolidayScheduledTask.java](/Users/bryan/Developement/YongJiBus/YongJiBus-Backend/src/main/java/com/yongjibus/scheduler/HolidayScheduledTask.java) 테스트 없음

## Strong Areas

- `chat` 메시지 저장 후 배달 분리와 after-commit 보장
- `report` 저장/메일 발행 분리와 rollback/after-commit 테스트
- `daytype` 캐시 무효화 회귀
- `vacation` 불변식 검증
- `jwt/auth service` 핵심 토큰/회원 상태 검증

## Weak Areas

- `controller` 경계 테스트 전반
- `security/filter` 테스트
- `scheduler` 테스트
- 일부 외부 연동 경계:
  - `GmailBounceController`
  - `FCMNotificationService`
  - `WebsocketStompErrorHandler`

## Recommended Test Priority

1. `controller` 테스트 보강
   - `AuthController`
   - `ChatController`
   - `MemberController`
   - `ReportController`
   - `ArrivalTimeController`
   - `DayTypeController`
   - `VacationController`
2. `security/websocket boundary` 테스트 보강
   - `JwtAuthenticationProcessingFilter`
   - `SecurityConfig`
   - `WebsocketStompErrorHandler`
   - `StompPreHandler`의 `SEND` / `DISCONNECT`
3. `scheduler/external boundary` 테스트 보강
   - `CalendarScheduledTask`
   - `HolidayScheduledTask`
   - `GmailBounceController`
   - `FCMNotificationService`

## Conclusion

현재 테스트는 “핵심 서비스 로직 회귀 방지” 관점에서는 꽤 좋아졌다.  
하지만 “실제 HTTP 요청, 인증 필터, 스케줄 실행, 외부 연동 경계까지 포함한 운영 경로”를 충분히 고정했다고 보기는 어렵다.  
따라서 프로젝트 전체 기준 최종 평가는 `부분 충분`이다.
