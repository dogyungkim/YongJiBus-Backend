# 장소 이미지 API

프론트엔드가 현재 백엔드 구현과 연동할 때 사용하는 장소 이미지 계약이다.

## 공통 규칙

- 관리자 이미지 업로드·삭제는 `ROLE_OPERATOR`가 필요한 관리자 API다. 사용자 장소 등록 요청에는 선택적으로 이미지를 첨부할 수 있지만, 사용자가 나중에 이미지만 추가·삭제하는 standalone API는 제공하지 않는다.
- JSON API의 성공 응답은 `{ "status": 200, "data": ... }` 형식이다. 실패 응답도 `{ "status": HTTP상태코드, "data": "메시지" }` 형식이며 별도 `code` 필드는 없다. `GET /place-images/{storageKey}`만 이미지 바이트를 직접 반환한다.
- 현재 로컬 저장소가 반환하는 URL은 `/place-images/...` 형태의 공개 상대 경로다. 프론트엔드의 API origin을 기준으로 해석한다.

```ts
const API_ORIGIN = "https://api.example.com";

function resolveApiUrl(path: string): string {
  return new URL(path, API_ORIGIN).toString();
}
```

이미지 원본은 서버에 보관하지 않는다. 서버는 업로드를 검증한 뒤 다음 두 JPEG만 생성·저장한다.

| 용도 | 제한 | JPEG 품질 |
| --- | ---: | ---: |
| `imageUrl` 표시용 | 긴 변 최대 2048px | 85% |
| `thumbnailUrl` 목록용 | 긴 변 최대 480px | 75% |

입력은 JPEG, PNG, WebP만 허용한다. 파일 시그니처와 실제 디코딩 결과를 함께 확인하며, 파일당 5MB 이하·긴 변 12,000px 이하·총 25MP 이하이어야 한다. 장소의 기존 이미지와 이번 요청 파일을 합쳐 최대 5장까지 가능하다. multipart 전송 자체의 최대 요청 크기는 30MB다. 작은 이미지는 확대하지 않고, JPEG EXIF 방향을 적용하며, 투명 영역은 흰색으로 합성한다.

## 이미지 항목

업로드와 관리자 장소 응답의 이미지 항목은 다음과 같다.

```ts
type PlaceImageItem = {
  id: number;
  imageUrl: string;       // 표시용 JPEG, 긴 변 최대 2048px
  thumbnailUrl: string;   // 목록용 JPEG, 긴 변 최대 480px
  sortOrder: number;      // 0부터 시작
};
```

신규 업로드에서는 `imageUrl`과 `thumbnailUrl`이 서로 다른 저장소 URL이다. V5 이전 레거시 이미지 행은 두 URL이 같을 수 있다.

## 사용자 장소 등록 요청에 이미지 첨부

```http
POST /places/requests
Authorization: Bearer {accessToken}
Content-Type: multipart/form-data
```

기존 JSON 요청은 그대로 사용할 수 있다. 이미지를 첨부할 때는 같은 경로로 multipart 요청을 보내며 다음 파트를 사용한다.

- `request`: 필수 JSON 파트(`Content-Type: application/json`), 기존 `PlaceRequest` 본문과 동일한 필드
- `images`: 선택 파일 파트, 같은 이름을 반복해 0~5개 전송. 0개라면 이 파트를 생략한다.

```bash
curl -X POST "$API_ORIGIN/places/requests" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -F 'request={"selectionProof":"eyJhbGciOiJIUzI1NiJ9...","displayName":"학생회관 카페","category":"CAFE","subcategory":null,"rating":5,"comment":"분위기가 좋아요."};type=application/json' \
  -F "images=@./cafe.png;type=image/png" \
  -F "images=@./interior.webp;type=image/webp"
```

프론트엔드에서는 JSON을 `application/json` Blob으로 넣고 파일마다 `images`를 반복해서 추가한다.

```ts
const form = new FormData();
form.append("request", new Blob([JSON.stringify(request)], { type: "application/json" }));
for (const file of files) {
  form.append("images", file, file.name);
}

const response = await fetch(resolveApiUrl("/places/requests"), {
  method: "POST",
  headers: { Authorization: `Bearer ${accessToken}` },
  body: form,
});
```

`FormData` 사용 시 `Content-Type`을 직접 지정하지 않는다. 브라우저가 multipart boundary를 포함해 설정한다. 사용자 이미지는 `PENDING` 장소에 등록할 때만 첨부할 수 있으며, 장소가 승인·거절·숨김 상태가 된 뒤에는 이 경로로 추가할 수 없다. 장소와 최초 평가 DB 요청을 먼저 저장한 뒤 이미지 파일을 붙이므로, 이미지 처리·저장이 실패해도 장소 요청과 평가는 이미 접수된 상태로 남을 수 있다. 같은 요청을 다시 보내면 평가를 upsert한 뒤 이미지 첨부를 다시 시도한다. 성공 응답은 이미지 배열이 아니라 기존 `PlaceRequestResult`다.

## 이미지 업로드

```http
POST /admin/places/{placeId}/images
Authorization: Bearer {operatorAccessToken}
Content-Type: multipart/form-data
```

multipart 필드 이름은 반드시 `images`다. 여러 파일은 같은 필드를 반복해서 보낸다. 요청 파일 순서대로 비어 있는 가장 낮은 `sortOrder`부터 배정한다.

```bash
API_ORIGIN="https://api.example.com"

curl -X POST "$API_ORIGIN/admin/places/42/images" \
  -H "Authorization: Bearer $OPERATOR_TOKEN" \
  -F "images=@./cafe.png;type=image/png" \
  -F "images=@./interior.webp;type=image/webp"
```

응답은 새로 등록한 항목만이 아니라 해당 장소의 현재 이미지 전체를 `sortOrder` 오름차순으로 반환한다.

```json
{
  "status": 200,
  "data": [
    {
      "id": 101,
      "imageUrl": "/place-images/550e8400-e29b-41d4-a716-446655440000.jpg",
      "thumbnailUrl": "/place-images/7c9e6679-7425-40de-944b-e07fc1f90ae7.jpg",
      "sortOrder": 0
    },
    {
      "id": 102,
      "imageUrl": "/place-images/8b7f3c1a-7f47-4d3e-9f3a-0a1b2c3d4e5f.jpg",
      "thumbnailUrl": "/place-images/1f2e3d4c-5b6a-47f8-9012-abcdefabcdef.jpg",
      "sortOrder": 1
    }
  ]
}
```

## 이미지 삭제

```http
DELETE /admin/places/{placeId}/images/{imageId}
Authorization: Bearer {operatorAccessToken}
```

```bash
curl -X DELETE "$API_ORIGIN/admin/places/42/images/101" \
  -H "Authorization: Bearer $OPERATOR_TOKEN"
```

성공 시 삭제된 이미지의 두 파일을 함께 정리하고 문자열 메시지를 반환한다.

```json
{
  "status": 200,
  "data": "장소 이미지가 삭제되었습니다."
}
```

삭제 응답에는 남은 이미지 목록이 포함되지 않는다. 프론트엔드는 성공 후 해당 `imageId`를 로컬 목록에서 제거하거나 관리자 장소 목록을 다시 조회한다. 표시용 키와 썸네일 키가 같은 레거시 행은 서버가 한 번만 삭제한다.

## 공개 장소 목록

```http
GET /places
```

인증 없이 호출할 수 있다. 응답의 `data`는 `PlaceSummary[]`이며 대표 이미지가 있으면 `thumbnailUrl`에 대표 `PlaceImageItem`의 썸네일 URL을 넣는다. 대표 이미지는 가장 낮은 `sortOrder`의 이미지다. 이미지가 없으면 `null`이다. 목록에는 표시용 `imageUrl`을 사용하지 않는다.

```ts
type PlaceSummary = {
  id: number;
  displayName: string;
  addressText: string;
  latitude: number;
  longitude: number;
  category: "FOOD" | "CAFE" | "BAR";
  subcategory: PlaceSubcategory | null;
  averageRating: number | null;
  reviewCount: number;
  representativeReview: string | null;
  kakaoPlaceUrl: string;
  thumbnailUrl: string | null;
};
```

```json
{
  "status": 200,
  "data": [
    {
      "id": 42,
      "displayName": "학생회관 카페",
      "addressText": "경기도 용인시 처인구 ...",
      "latitude": 37.2242,
      "longitude": 127.18766,
      "category": "CAFE",
      "subcategory": null,
      "averageRating": 4.5,
      "reviewCount": 12,
      "representativeReview": "분위기가 편해요.",
      "kakaoPlaceUrl": "https://place.map.kakao.com/123456",
      "thumbnailUrl": "/place-images/7c9e6679-7425-40de-944b-e07fc1f90ae7.jpg"
    }
  ]
}
```

## 공개 장소 상세

```http
GET /places/{placeId}
```

인증 없이 호출할 수 있다. 응답은 `PlaceSummary` 필드를 그대로 포함하고, 대표 `thumbnailUrl`과 모든 이미지의 표시용 URL을 `imageUrls`에 순서대로 반환한다. 상세 화면의 큰 이미지에는 `imageUrls`를 사용하고, 목록·대표 영역에는 `thumbnailUrl`을 사용한다.

```ts
type MyReview = {
  id: number;
  rating: number;
  comment: string;
  status: "PENDING" | "APPROVED" | "REJECTED" | "HIDDEN";
  createdAt: string;
  updatedAt: string;
};

type PlaceDetail = PlaceSummary & {
  imageUrls: string[];          // sortOrder 오름차순의 표시용 URL
  myReview: MyReview | null;
};
```

```json
{
  "status": 200,
  "data": {
    "id": 42,
    "displayName": "학생회관 카페",
    "addressText": "경기도 용인시 처인구 ...",
    "latitude": 37.2242,
    "longitude": 127.18766,
    "category": "CAFE",
    "subcategory": null,
    "averageRating": 4.5,
    "reviewCount": 12,
    "representativeReview": "분위기가 편해요.",
    "kakaoPlaceUrl": "https://place.map.kakao.com/123456",
    "thumbnailUrl": "/place-images/7c9e6679-7425-40de-944b-e07fc1f90ae7.jpg",
    "imageUrls": [
      "/place-images/550e8400-e29b-41d4-a716-446655440000.jpg",
      "/place-images/8b7f3c1a-7f47-4d3e-9f3a-0a1b2c3d4e5f.jpg"
    ],
    "myReview": null
  }
}
```

## 관리자 장소 응답

다음 `AdminPlace` 응답은 모두 `images` 배열을 포함한다. 모든 경로에 `ROLE_OPERATOR`가 필요하다.

```http
POST /admin/places
GET  /admin/places?status=PENDING&page=0&size=20
POST /admin/places/{placeId}/approve
POST /admin/places/{placeId}/reject
POST /admin/places/{placeId}/hide
POST /admin/places/{placeId}/restore
```

```ts
type AdminPlaceImageFields = {
  images: PlaceImageItem[]; // sortOrder 오름차순
};
```

관리자 목록은 일반적인 `SliceResponse`로 감싸져 다음처럼 `data.content[].images`에서 읽는다. 아래 JSON은 이미지 계약을 보여주기 위해 `AdminPlace`의 나머지 필드를 생략한 예시다.

```json
{
  "status": 200,
  "data": {
    "content": [
      {
        "id": 42,
        "displayName": "학생회관 카페",
        "status": "APPROVED",
        "images": [
          {
            "id": 101,
            "imageUrl": "/place-images/550e8400-e29b-41d4-a716-446655440000.jpg",
            "thumbnailUrl": "/place-images/7c9e6679-7425-40de-944b-e07fc1f90ae7.jpg",
            "sortOrder": 0
          }
        ]
      }
    ],
    "hasNext": false,
    "number": 0,
    "size": 20
  }
}
```

업로드 API 자체는 `data`에 `PlaceImageItem[]`를 직접 반환하고, 관리자 장소 생성·목록·상태 변경 응답은 `AdminPlace` 안에 `images`를 포함한다.

## 오류

실패 응답은 모두 다음 모양이다.

```json
{
  "status": 400,
  "data": "장소 이미지는 최대 5개까지 등록할 수 있습니다."
}
```

아래 표의 `ErrorCode`는 소스에서 사용하는 이름이며, 실제 JSON에는 `data`의 메시지만 노출된다.

| HTTP | ErrorCode | 발생 예 | `data` 메시지 |
| ---: | --- | --- | --- |
| 400 | `INVALID_PLACE_IMAGE` | MIME·시그니처·디코딩·크기·해상도·픽셀 제한 위반, 관리자 API의 `images` 누락, multipart 요청 30MB 초과 | `JPEG, PNG, WEBP 형식의 5MB 이하 이미지만 등록할 수 있습니다.` |
| 400 | `INVALID_REQUEST` | 사용자 multipart 요청의 필수 `request` 파트 누락 | `잘못된 요청입니다.` |
| 400 | `TOO_MANY_PLACE_IMAGES` | 기존 이미지와 신규 파일의 합이 5장 초과, 또는 요청 파일이 5장 초과 | `장소 이미지는 최대 5개까지 등록할 수 있습니다.` |
| 401 | `UNAUTHORIZED` | 관리자 API에 인증 토큰 없음 | `인증된 사용자가 아닙니다.` |
| 401 | `INVALID_ACCESS_TOKEN` | 잘못된 액세스 토큰 또는 Authorization 형식 | `유효하지 않은 토큰입니다.` |
| 403 | `OPERATOR_REQUIRED` | 로그인했지만 운영자 권한 없음 | `운영자 권한이 필요합니다.` |
| 404 | `PLACE_NOT_FOUND` | 존재하지 않는 장소 | `장소를 찾을 수 없습니다.` |
| 404 | `PLACE_IMAGE_NOT_FOUND` | 해당 장소의 이미지가 없음 | `장소 이미지를 찾을 수 없습니다.` |
| 500 | `PLACE_IMAGE_STORAGE_FAILED` | 이미지 저장소 저장·삭제 실패 | `장소 이미지 저장에 실패했습니다.` |

이미지 처리·저장·DB 기록 중 실패하면 해당 요청에서 생성한 파일은 서버가 최선으로 정리하며, 성공 응답을 받은 경우에만 반환된 URL을 사용한다.

## 순서와 프론트엔드 연동

- 이미지 순서를 바꾸는 API는 현재 없다.
- 업로드 파일은 multipart에 추가된 순서대로 비어 있는 가장 낮은 `sortOrder`를 받는다. 예를 들어 `0, 2`가 사용 중이면 신규 이미지는 `1`부터 들어간다.
- 업로드 성공 시 반환된 전체 배열로 관리자 화면의 목록 상태를 교체한다. 삭제 성공 시에는 응답의 문자열을 확인한 뒤 해당 ID를 제거하거나 목록을 다시 조회한다.
- 장소 목록과 관리자 카드·목록 미리보기에서는 `thumbnailUrl`, 장소 상세의 큰 이미지와 관리자 전체 크기 미리보기에서는 `imageUrl`을 사용한다.
- URL은 파일명이나 원본 확장자로 조합하지 말고 응답값 그대로 `API_ORIGIN`과 결합한다. 로컬 기본 경로는 `GET /place-images/{storageKey}`이며 인증 없이 공개된다.

```ts
async function uploadPlaceImages(
  placeId: number,
  files: File[],
  accessToken: string,
): Promise<PlaceImageItem[]> {
  const form = new FormData();
  for (const file of files) {
    form.append("images", file, file.name);
  }

  const response = await fetch(
    resolveApiUrl(`/admin/places/${placeId}/images`),
    {
      method: "POST",
      headers: { Authorization: `Bearer ${accessToken}` },
      body: form,
    },
  );
  const body = await response.json();
  if (!response.ok) {
    throw new Error(body.data ?? "이미지 업로드에 실패했습니다.");
  }
  return body.data;
}
```

`FormData` 사용 시 `Content-Type`을 직접 지정하지 않는다. 브라우저가 multipart boundary를 포함해 설정한다. 업로드 전 클라이언트에서 파일 선택 개수·크기·미리보기를 안내할 수 있지만, 최종 검증과 장소별 잔여 개수 판단은 서버 응답을 기준으로 처리한다. 위 관리자 API와 사용자 장소 등록 요청의 이미지 처리 제한은 동일하다.
