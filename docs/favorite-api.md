# 목차

1. 관심 매물 개념
2. DB 구조
3. API 목록
4. Swagger 테스트 방법
5. 각 API 상세
6. 에러 코드
7. 요청/응답 흐름
8. 구현 설명

---

# 관심 매물 (Favorite)

## 개념

사용자가 마음에 드는 매물을 저장해두고 나중에 다시 볼 수 있는 기능.

`(userId, propertyId)` 조합을 DB에 저장하는 단순한 구조다.
같은 사용자가 같은 매물을 중복 추가할 수 없도록 DB에 UNIQUE 제약이 걸려 있다.

---

# DB 구조

```sql
CREATE TABLE favorites (
    favorite_id BIGINT   NOT NULL AUTO_INCREMENT,
    user_id     BIGINT   NOT NULL,
    property_id BIGINT   NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (favorite_id),
    UNIQUE KEY uq_favorites (user_id, property_id)  -- 중복 추가 방지
);
```

- `user_id` + `property_id` 조합에 UNIQUE 제약 → 같은 매물을 두 번 추가하면 DB 레벨에서 거부
- `ON DELETE CASCADE` → 사용자나 매물이 삭제되면 관심 목록도 자동 삭제

---

# API 목록

| 메서드 | URI | 설명 | 권한 |
| --- | --- | --- | --- |
| POST | /api/favorites/{propertyId} | 관심 매물 추가 | 로그인 필요 |
| DELETE | /api/favorites/{propertyId} | 관심 매물 삭제 | 로그인 필요 |
| GET | /api/favorites | 내 관심 매물 목록 조회 | 로그인 필요 |

> 세 API 모두 **로그인하지 않으면 401 UNAUTHORIZED** 반환

---

# Swagger 테스트 방법

## 1단계 — 회원가입

`POST /api/auth/signup`

```json
{
  "email": "buyer@test.com",
  "password": "password123!",
  "name": "테스터",
  "nickname": "tester",
  "role": "BUYER"
}
```

## 2단계 — 로그인

`POST /api/auth/login`

```json
{
  "email": "buyer@test.com",
  "password": "password123!"
}
```

응답에서 `accessToken` 값 복사

```json
{
  "data": {
    "accessToken": "eyJhbGci...",
    "refreshToken": "..."
  }
}
```

## 3단계 — 토큰 등록

Swagger 우측 상단 **Authorize** 버튼 클릭
→ `accessToken` 값 그대로 붙여넣기 (`Bearer` 접두어 불필요, 자동 추가됨)
→ **Authorize** 클릭

이후 모든 API 요청에 `Authorization: Bearer {token}` 헤더가 자동으로 붙는다.

## 4단계 — 테스트 순서

1. `GET /api/properties` → 매물 목록에서 `propertyId` 확인
2. `POST /api/favorites/{propertyId}` → 관심 매물 추가
3. `GET /api/favorites` → 목록에 추가됐는지 확인
4. `POST /api/favorites/{propertyId}` 동일 ID 재시도 → `FAVORITE_ALREADY_EXISTS` 에러 확인
5. `DELETE /api/favorites/{propertyId}` → 삭제
6. `GET /api/favorites` → 목록이 비어있는지 확인

---

# 각 API 상세

## POST /api/favorites/{propertyId} — 관심 매물 추가

요청 바디 없음. Path Variable로 `propertyId`만 넘긴다.

**성공 응답 (200)**
```json
{
  "success": true,
  "message": "관심 매물에 추가되었습니다.",
  "data": null
}
```

**실패 응답 — 중복 추가 (409)**
```json
{
  "success": false,
  "message": "이미 관심 매물로 등록된 매물입니다.",
  "errorCode": "FAVORITE_ALREADY_EXISTS"
}
```

---

## DELETE /api/favorites/{propertyId} — 관심 매물 삭제

요청 바디 없음. Path Variable로 `propertyId`만 넘긴다.

**성공 응답 (200)**
```json
{
  "success": true,
  "message": "관심 매물에서 삭제되었습니다.",
  "data": null
}
```

**실패 응답 — 목록에 없는 매물 삭제 시도 (404)**
```json
{
  "success": false,
  "message": "관심 매물 목록에 없는 매물입니다.",
  "errorCode": "FAVORITE_NOT_FOUND"
}
```

---

## GET /api/favorites — 내 관심 매물 목록 조회

**성공 응답 (200)**
```json
{
  "success": true,
  "message": "관심 매물 목록을 조회했습니다.",
  "data": [
    {
      "favoriteId": 1,
      "property": {
        "propertyId": 10,
        "title": "강남 원룸",
        "sido": "서울특별시",
        "gugun": "강남구",
        "dong": "역삼동",
        "rentType": "MONTHLY",
        "deposit": 500,
        "monthlyRent": 70,
        "area": 33.5,
        "status": "APPROVED"
      },
      "createdAt": "2026-06-07T10:00:00"
    }
  ]
}
```

- 내 계정으로 추가한 관심 매물만 반환 (다른 사용자 목록은 보이지 않음)
- 추가한 매물이 없으면 빈 배열 `[]` 반환
- 매물 가격 필드(`deposit`, `monthlyRent`)는 만원 단위다. 예: `deposit: 500`은 보증금 500만원, `monthlyRent: 70`은 월세 70만원.

---

# 에러 코드

| ErrorCode | HTTP 상태 | 설명 |
| --- | --- | --- |
| `UNAUTHORIZED` | 401 | 토큰 없이 접근 |
| `FAVORITE_ALREADY_EXISTS` | 409 | 이미 관심 매물로 등록된 매물 재추가 |
| `FAVORITE_NOT_FOUND` | 404 | 관심 목록에 없는 매물 삭제 시도 |
| `PROPERTY_NOT_FOUND` | 404 | 존재하지 않는 매물 ID |

---

# 요청/응답 흐름

## 추가 흐름

```
POST /api/favorites/10
Authorization: Bearer eyJhbGci...
        ↓
JwtAuthenticationFilter: 토큰 검증 → userId 추출
        ↓
FavoriteController.addFavorite(propertyId=10, userId=1)
        ↓
FavoriteService.addFavorite(userId=1, propertyId=10)
  - favorites 테이블에 (userId=1, propertyId=10) 조합 존재 여부 확인
  - 이미 있으면 → FAVORITE_ALREADY_EXISTS 예외
  - 없으면 → Property 조회 후 Favorite 엔티티 저장
        ↓
INSERT INTO favorites (user_id, property_id) VALUES (1, 10)
        ↓
{ "success": true, "message": "관심 매물에 추가되었습니다." }
```

## 목록 조회 흐름

```
GET /api/favorites
Authorization: Bearer eyJhbGci...
        ↓
JwtAuthenticationFilter: 토큰 검증 → userId 추출
        ↓
FavoriteController.getMyFavorites(userId=1)
        ↓
FavoriteService.getMyFavorites(userId=1)
        ↓
SELECT * FROM favorites WHERE user_id = 1
        ↓
각 Favorite → FavoriteResponse.from() → PropertyResponse 포함
        ↓
{ "success": true, "data": [...] }
```

---

# 구현 설명

## Favorite 엔티티 — userId는 Long, property는 @ManyToOne

```java
@Column(name = "user_id", nullable = false)
private Long userId;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "property_id", nullable = false)
private Property property;
```

`userId`와 `property`를 다르게 저장한 이유가 있다.

목록 조회 시 응답에 매물 상세 정보(제목, 지역, 가격 등)가 포함돼야 한다.
`property`를 엔티티로 가져야 `PropertyResponse.from(favorite.getProperty())`가 가능하다.
반면 User 정보는 응답에 필요 없어서 Long으로만 저장했다.

`BaseTimeEntity`를 상속하지 않은 이유는 `favorites` 테이블에 `updated_at`이 없기 때문이다.
`@CreatedDate`만 직접 선언해서 `created_at`만 관리한다.

---

## FavoriteRepository — 메서드명이 긴 이유

```java
boolean existsByUserIdAndPropertyPropertyId(Long userId, Long propertyId);
```

Spring Data JPA 네이밍 규칙으로 자동 쿼리를 생성하는 방식이다.

`PropertyPropertyId`는 `property.propertyId`를 탐색하라는 의미다.
`property`(필드명) → `PropertyId`(그 안의 필드명)을 대문자로 연결한 것이다.

---

## ErrorCode — 중복 추가가 409인 이유

```java
FAVORITE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 관심 매물로 등록된 매물입니다."),
```

`400 BAD_REQUEST`가 아닌 `409 CONFLICT`를 쓴 이유는, 요청 형식 자체는 올바른데 **현재 서버 상태와 충돌**하는 상황이기 때문이다.

---

## FavoriteService — deleteBy 대신 find 후 delete를 쓴 이유

```java
Favorite favorite = favoriteRepository.findByUserIdAndPropertyPropertyId(userId, propertyId)
        .orElseThrow(() -> new BusinessException(ErrorCode.FAVORITE_NOT_FOUND));
favoriteRepository.delete(favorite);
```

`deleteBy...`를 쓰면 삭제 대상이 없어도 예외 없이 조용히 넘어간다.
목록에 없는 매물을 삭제하려 할 때 `FAVORITE_NOT_FOUND` 에러를 반환해야 하므로, 먼저 조회해서 없으면 예외를 던지는 방식을 택했다.

---

## SecurityConfig를 건드리지 않은 이유

```java
.anyRequest().authenticated()
```

기존 SecurityConfig 마지막 줄이 모든 나머지 요청에 인증을 요구한다.
`/api/favorites/**`는 별도 규칙 없이 이 줄에 해당한다.
BUYER/AGENT/ADMIN 구분 없이 인증된 사용자면 모두 접근 가능하므로 추가할 내용이 없었다.
