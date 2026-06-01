# Authentication and User API

## 공통 응답 형식

### 성공 응답

```json
{
  "success": true,
  "message": "요청이 성공했습니다.",
  "data": {}
}
```

### 실패 응답

```json
{
  "success": false,
  "message": "에러 메시지",
  "errorCode": "ERROR_CODE"
}
```

## 인증 및 권한 API

| 메서드 | URI | 설명 | 인증 | 권한 | 비고 |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/api/auth/signup` | 회원가입 | 불필요 | 비회원 | 가입 시 `BUYER`, `AGENT` 선택 가능 |
| `POST` | `/api/auth/login` | 로그인 | 불필요 | 비회원 | Access Token, Refresh Token 발급 |
| `POST` | `/api/auth/logout` | 로그아웃 | 필요 | `BUYER`, `AGENT`, `ADMIN` | 클라이언트 토큰 삭제 기준, Refresh Token 무효화 가능 |
| `POST` | `/api/auth/refresh` | 토큰 재발급 | 불필요 | Refresh Token 보유 사용자 | Refresh Token으로 Access Token 재발급 |
| `GET` | `/api/auth/me` | 내 인증 정보 조회 | 필요 | `BUYER`, `AGENT`, `ADMIN` | 사용자 ID, 이메일, 닉네임, 역할 포함 |

## 사용자 정보 API

| 메서드 | URI | 설명 | 인증 | 권한 | 비고 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/users/me` | 내 정보 조회 | 필요 | `BUYER`, `AGENT`, `ADMIN` | 사용자 프로필 상세 조회 |
| `PATCH` | `/api/users/me` | 내 정보 수정 | 필요 | `BUYER`, `AGENT`, `ADMIN` | 닉네임, 이름, 전화번호 수정 |
| `PATCH` | `/api/users/me/status` | 내 계정 상태 변경 | 필요 | `BUYER`, `AGENT` | 비활성화, 탈퇴 처리 |
| `PATCH` | `/api/users/me/role` | 내 역할 변경 | 필요 | `BUYER` | `BUYER -> AGENT` 변경 허용 |

## 요청 및 응답 상세

### POST `/api/auth/signup`

회원가입을 수행한다. 일반 회원가입에서는 `provider`를 서버에서 `LOCAL`로 처리한다.

#### Request

```json
{
  "email": "user@example.com",
  "password": "password123!",
  "name": "홍길동",
  "nickname": "길동",
  "phoneNumber": "01012345678",
  "role": "BUYER"
}
```

#### Validation

| 필드 | 규칙 |
| --- | --- |
| `email` | 필수, 이메일 형식, 중복 불가 |
| `password` | 필수, 암호화 저장 |
| `name` | 필수 |
| `nickname` | 필수 |
| `phoneNumber` | `BUYER`는 선택, `AGENT`는 필수 |
| `role` | 필수, `BUYER` 또는 `AGENT`만 허용 |

#### Response Data

```json
{
  "id": 1,
  "email": "user@example.com",
  "name": "홍길동",
  "nickname": "길동",
  "phoneNumber": "01012345678",
  "role": "BUYER",
  "status": "ACTIVE",
  "provider": "LOCAL"
}
```

### POST `/api/auth/login`

이메일과 비밀번호로 로그인한다.

#### Request

```json
{
  "email": "user@example.com",
  "password": "password123!"
}
```

#### Response Data

```json
{
  "accessToken": "access.jwt.token",
  "refreshToken": "refresh.jwt.token",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": 1,
    "email": "user@example.com",
    "nickname": "길동",
    "role": "BUYER",
    "status": "ACTIVE"
  }
}
```

### POST `/api/auth/logout`

로그아웃을 수행한다.

#### Request

```json
{
  "refreshToken": "refresh.jwt.token"
}
```

#### Response Data

```json
null
```

### POST `/api/auth/refresh`

Refresh Token으로 Access Token을 재발급한다.

#### Request

```json
{
  "refreshToken": "refresh.jwt.token"
}
```

#### Response Data

```json
{
  "accessToken": "new.access.jwt.token",
  "refreshToken": "new.refresh.jwt.token",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

### GET `/api/auth/me`

현재 토큰의 인증 주체 정보를 조회한다.

#### Response Data

```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "길동",
  "role": "BUYER",
  "status": "ACTIVE"
}
```

### GET `/api/users/me`

내 사용자 상세 정보를 조회한다.

#### Response Data

```json
{
  "id": 1,
  "email": "user@example.com",
  "name": "홍길동",
  "nickname": "길동",
  "phoneNumber": "01012345678",
  "role": "BUYER",
  "status": "ACTIVE",
  "provider": "LOCAL",
  "lastLoginAt": "2026-05-29T15:00:00",
  "createdAt": "2026-05-29T14:00:00",
  "updatedAt": "2026-05-29T15:00:00"
}
```

### PATCH `/api/users/me`

내 사용자 정보를 수정한다.

#### Request

```json
{
  "name": "홍길동",
  "nickname": "새닉네임",
  "phoneNumber": "01098765432"
}
```

#### Response Data

```json
{
  "id": 1,
  "email": "user@example.com",
  "name": "홍길동",
  "nickname": "새닉네임",
  "phoneNumber": "01098765432",
  "role": "BUYER",
  "status": "ACTIVE"
}
```

### PATCH `/api/users/me/status`

내 계정 상태를 변경한다. 사용자는 본인 계정을 비활성화하거나 탈퇴 처리할 수 있다.

#### Request

```json
{
  "status": "INACTIVE"
}
```

#### Validation

| 필드 | 규칙 |
| --- | --- |
| `status` | `INACTIVE`, `DELETED`만 허용 |

#### Response Data

```json
{
  "id": 1,
  "status": "INACTIVE"
}
```

### PATCH `/api/users/me/role`

내 역할을 변경한다.

#### Request

```json
{
  "role": "AGENT",
  "phoneNumber": "01012345678"
}
```

#### Validation

| 필드 | 규칙 |
| --- | --- |
| `role` | `AGENT`만 허용 |
| `phoneNumber` | `AGENT` 변경 시 필수 |

#### Response Data

```json
{
  "id": 1,
  "role": "AGENT",
  "phoneNumber": "01012345678"
}
```

## 권한 정책

| 기능 | 허용 권한 |
| --- | --- |
| 회원가입 | 비회원 |
| 로그인 | 비회원 |
| 로그아웃 | `BUYER`, `AGENT`, `ADMIN` |
| 토큰 재발급 | 유효한 Refresh Token 보유 사용자 |
| 내 인증 정보 조회 | `BUYER`, `AGENT`, `ADMIN` |
| 내 사용자 정보 조회 | `BUYER`, `AGENT`, `ADMIN` |
| 내 사용자 정보 수정 | `BUYER`, `AGENT`, `ADMIN` |
| 내 계정 상태 변경 | `BUYER`, `AGENT` |
| 내 역할 변경 | `BUYER` |

## 에러 코드 초안

| errorCode | 설명 |
| --- | --- |
| `DUPLICATE_EMAIL` | 이미 가입된 이메일 |
| `INVALID_CREDENTIALS` | 이메일 또는 비밀번호 불일치 |
| `INVALID_TOKEN` | 유효하지 않은 토큰 |
| `EXPIRED_TOKEN` | 만료된 토큰 |
| `UNAUTHORIZED` | 인증 필요 |
| `FORBIDDEN` | 권한 없음 |
| `USER_NOT_FOUND` | 사용자를 찾을 수 없음 |
| `INACTIVE_USER` | 비활성화된 계정 |
| `BANNED_USER` | 이용 제한된 계정 |
| `DELETED_USER` | 탈퇴 처리된 계정 |
| `INVALID_ROLE` | 허용되지 않은 역할 |
| `INVALID_STATUS` | 허용되지 않은 계정 상태 |
| `PHONE_NUMBER_REQUIRED` | 전화번호 필수 |
