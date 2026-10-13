# 채팅 API 문서

YongJiBus 백엔드의 채팅방, 메시지, WebSocket, FCM 토큰 관련 API를 정리한 문서입니다.

## 개요

- REST Base Path: `/chat`
- WebSocket Endpoint: `/ws-stomp`
- 메시지 브로커 구독 Prefix: `/sub`
- 메시지 발행 Prefix: `/pub`
- 인증 방식:
  - REST API: `Authorization: Bearer <accessToken>` 헤더 필요
  - WebSocket: STOMP `CONNECT` 프레임에 `Authorization: Bearer <accessToken>` 헤더 필요

## 빠른 사용 흐름

1. 로그인해서 `accessToken`을 발급받습니다.
2. `GET /chat/rooms` 또는 `GET /chat/rooms/my`로 채팅방 목록을 조회합니다.
3. 필요하면 `POST /chat/rooms`로 방을 만들거나 `POST /chat/rooms/{roomId}`로 참여합니다.
4. 이전 메시지가 필요하면 `GET /chat/rooms/{roomId}/messages`로 조회합니다.
5. WebSocket으로 `/ws-stomp`에 연결한 뒤 STOMP `CONNECT` 헤더에 JWT를 포함합니다.
6. `/sub/chat/room/{roomId}`를 구독합니다.
7. `/pub/chat/message`로 메시지를 전송합니다.
8. 퇴장 시 `DELETE /chat/rooms/{roomId}/leave`를 호출합니다.

## 공통 응답 형식

성공 응답:

```json
{
  "status": 200,
  "data": {}
}
```

실패 응답:

```json
{
  "status": 403,
  "data": "채팅방 참여자가 아닙니다."
}
```

## 데이터 구조

### ChatRoomResponseDTO

```json
{
  "id": 1,
  "name": "인문캠 08:30 카풀방",
  "departureTime": "08:30:00",
  "userCount": 3,
  "createdAt": "2026-03-23T08:12:34"
}
```

### ChatMessageResponseDTO

```json
{
  "id": 10,
  "messageType": "MESSAGE",
  "content": "정문에서 만나요",
  "sender": "bryan",
  "roomId": 1,
  "createdAt": "2026-03-23T08:31:02"
}
```

`messageType`은 아래 네 가지 값을 가질 수 있습니다.

- `MESSAGE`: 일반 채팅 메시지
- `ENTER`: 입장 메시지
- `LEAVE`: 퇴장 메시지
- `SYSTEM`: 시스템 안내 메시지

## REST API

### 1. 참여 가능한 채팅방 목록 조회

- Method: `GET`
- Path: `/chat/rooms`
- 설명: 현재 로그인한 사용자가 아직 참여하지 않은 채팅방 목록을 조회합니다.
- 인증: 필요

응답 예시:

```json
{
  "status": 200,
  "data": [
    {
      "id": 1,
      "name": "인문캠 08:30 카풀방",
      "departureTime": "08:30:00",
      "userCount": 3,
      "createdAt": "2026-03-23T08:12:34"
    }
  ]
}
```

주요 오류:

- `401`: 인증되지 않은 사용자

### 2. 내가 참여 중인 채팅방 목록 조회

- Method: `GET`
- Path: `/chat/rooms/my`
- 설명: 현재 로그인한 사용자가 참여 중인 채팅방 목록을 조회합니다.
- 인증: 필요

응답 예시:

```json
{
  "status": 200,
  "data": [
    {
      "id": 3,
      "name": "자연캠 09:00 카풀방",
      "departureTime": "09:00:00",
      "userCount": 4,
      "createdAt": "2026-03-23T07:55:10"
    }
  ]
}
```

### 3. 채팅방 생성

- Method: `POST`
- Path: `/chat/rooms`
- 설명: 새로운 채팅방을 생성하고, 생성자는 자동으로 해당 방에 참여합니다.
- 인증: 필요

요청 예시:

```json
{
  "name": "인문캠 08:30 카풀방",
  "departureTime": "08:30:00"
}
```

응답 예시:

```json
{
  "status": 200,
  "data": {
    "id": 1,
    "name": "인문캠 08:30 카풀방",
    "departureTime": "08:30:00",
    "userCount": 1,
    "createdAt": "2026-03-23T08:12:34"
  }
}
```

비고:

- 방 생성 직후 시스템 안내 메시지가 저장되고 `/sub/chat/room/{roomId}`로 발행됩니다.

### 4. 채팅방 참여

- Method: `POST`
- Path: `/chat/rooms/{roomId}`
- 설명: 지정한 채팅방에 참여합니다.
- 인증: 필요

응답 예시:

```json
{
  "status": 200,
  "data": {
    "id": 1,
    "name": "인문캠 08:30 카풀방",
    "departureTime": "08:30:00",
    "userCount": 4,
    "createdAt": "2026-03-23T08:12:34"
  }
}
```

주요 오류:

- `400`: 채팅방 인원이 가득 찼습니다.
- `401`: 인증되지 않은 사용자
- `404`: 채팅방을 찾을 수 없습니다.

비고:

- 최대 정원은 5명입니다.
- 이미 참여 중인 사용자가 다시 호출하면 기존 채팅방 정보를 그대로 반환합니다.
- 참여 성공 시 `ENTER` 타입 메시지가 저장되고 브로드캐스트됩니다.

### 5. 채팅 메시지 조회

- Method: `GET`
- Path: `/chat/rooms/{roomId}/messages`
- 설명: 특정 채팅방의 메시지 기록을 페이지네이션으로 조회합니다.
- 인증: 필요
- Query Params:
  - `page`: 페이지 번호
  - `size`: 페이지 크기
  - `sort`: 정렬 조건

요청 예시:

```http
GET /chat/rooms/1/messages?page=0&size=20
Authorization: Bearer <accessToken>
```

응답 예시:

```json
{
  "status": 200,
  "data": {
    "content": [
      {
        "id": 10,
        "messageType": "MESSAGE",
        "content": "정문에서 만나요",
        "sender": "bryan",
        "roomId": 1,
        "createdAt": "2026-03-23T08:31:02"
      }
    ],
    "hasNext": true,
    "number": 0,
    "size": 20
  }
}
```

주요 오류:

- `401`: 인증되지 않은 사용자
- `403`: 채팅방 참여자가 아닙니다.
- `404`: 채팅방을 찾을 수 없습니다.

비고:

- 현재 사용자가 해당 채팅방에 참여한 시점 이후의 메시지만 조회됩니다.

### 6. 채팅방 정보 조회

- Method: `GET`
- Path: `/chat/rooms/{roomId}`
- 설명: 특정 채팅방의 상세 정보를 조회합니다.
- 인증: 필요

응답 예시:

```json
{
  "status": 200,
  "data": {
    "id": 1,
    "name": "인문캠 08:30 카풀방",
    "departureTime": "08:30:00",
    "userCount": 3,
    "createdAt": "2026-03-23T08:12:34"
  }
}
```

주요 오류:

- `404`: 채팅방을 찾을 수 없습니다.

비고:

- 현재 구현 기준으로는 방 존재 여부만 확인하며, 참여 중인지 여부는 검사하지 않습니다.

### 7. 채팅방 퇴장

- Method: `DELETE`
- Path: `/chat/rooms/{roomId}/leave`
- 설명: 참여 중인 채팅방에서 퇴장합니다.
- 인증: 필요

응답 예시:

```json
{
  "status": 200,
  "data": "채팅방 퇴장 성공"
}
```

주요 오류:

- `401`: 인증되지 않은 사용자
- `403`: 채팅방 참여자가 아닙니다.
- `404`: 채팅방을 찾을 수 없습니다.

비고:

- 마지막 참여자가 퇴장하면 해당 채팅방은 삭제됩니다.
- 퇴장 성공 시 `LEAVE` 타입 메시지가 브로드캐스트됩니다.

### 8. FCM 토큰 등록

- Method: `POST`
- Path: `/chat/fcm-token`
- 설명: 푸시 알림 수신을 위한 FCM 토큰을 등록합니다.
- 인증: 필요

요청 예시:

```json
{
  "token": "fcm-device-token"
}
```

응답 예시:

```json
{
  "status": 200,
  "data": "FCM 토큰 등록 성공"
}
```

### 9. FCM 토큰 삭제

- Method: `POST`
- Path: `/chat/fcm-token/remove`
- 설명: 등록된 FCM 토큰을 비활성화합니다.
- 인증: 필요

응답 예시:

```json
{
  "status": 200,
  "data": "FCM 토큰 삭제 성공"
}
```

주요 오류:

- `401`: 인증되지 않은 사용자
- `404`: FCM 토큰을 찾을 수 없습니다.

## WebSocket / STOMP

### 연결 정보

- Endpoint: `/ws-stomp`
- Protocol: STOMP over WebSocket
- SockJS 사용 여부: 사용 안 함

브라우저 또는 앱에서 실제 연결 URL은 아래 형태입니다.

```text
ws://<host>/ws-stomp
wss://<host>/ws-stomp
```

### 1. CONNECT

핸드셰이크 엔드포인트는 열려 있지만, 실제 인증은 STOMP `CONNECT` 프레임에서 처리합니다.

헤더 예시:

```json
{
  "Authorization": "Bearer <accessToken>"
}
```

주의:

- `Authorization` 헤더가 없으면 연결이 거절됩니다.
- `Bearer ` 접두사가 없으면 연결이 거절됩니다.
- 유효하지 않은 토큰이면 연결이 거절됩니다.

### 2. SUBSCRIBE

- Destination: `/sub/chat/room/{roomId}`
- 설명: 특정 채팅방의 실시간 메시지를 구독합니다.

예시:

```text
/sub/chat/room/1
```

주의:

- 해당 채팅방의 활성 참여자만 구독할 수 있습니다.
- 참여자가 아니면 `CHAT_ROOM_FORBIDDEN` 오류가 발생합니다.

### 3. SEND

- Destination: `/pub/chat/message`
- 설명: 채팅 메시지를 전송합니다.

본문 예시:

```json
{
  "content": "정문에서 만나요",
  "roomId": 1
}
```

수신 예시:

```json
{
  "id": 11,
  "messageType": "MESSAGE",
  "content": "정문에서 만나요",
  "sender": "bryan",
  "roomId": 1,
  "createdAt": "2026-03-23T08:31:02"
}
```

### 4. ERROR Frame

STOMP 처리 중 오류가 발생하면 서버는 `ERROR` 프레임을 반환합니다.

현재 구현에서 `body`는 객체가 아니라 메시지 문자열을 JSON 문자열 형태로 내려줍니다.

예시:

```json
"채팅방 참여자가 아닙니다."
```

주요 WebSocket 오류 메시지:

- `"인증된 사용자가 아닙니다."`
- `"유효하지 않은 토큰입니다."`
- `"채팅방을 찾을 수 없습니다."`
- `"채팅방 참여자가 아닙니다."`

## 프론트엔드 예시

```ts
import { Client } from '@stomp/stompjs';

const client = new Client({
  brokerURL: 'wss://api.example.com/ws-stomp',
  connectHeaders: {
    Authorization: `Bearer ${accessToken}`,
  },
  reconnectDelay: 5000,
});

client.onConnect = () => {
  client.subscribe(`/sub/chat/room/${roomId}`, frame => {
    const message = JSON.parse(frame.body);
    console.log(message);
  });

  client.publish({
    destination: '/pub/chat/message',
    body: JSON.stringify({
      content: '정문에서 만나요',
      roomId,
    }),
  });
};

client.onStompError = frame => {
  console.error('STOMP error', frame.body);
};

client.activate();
```

## 참고

- Swagger UI: `/swagger-ui/index.html`
- OpenAPI JSON: `/v3/api-docs`
