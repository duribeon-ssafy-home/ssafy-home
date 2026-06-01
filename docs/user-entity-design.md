# User Entity Design

## 개요

사용자 엔티티는 로그인 주체, 서비스 권한, 기본 프로필 정보를 관리한다.
OAuth 소셜 로그인 확장을 고려하여 비밀번호와 OAuth provider 식별자는 nullable로 설계한다.

## User 필드

| 필드 | 필수 여부 | 타입 예시 | 설명 | 합의 내용 |
| --- | --- | --- | --- | --- |
| `id` | 필수 | `Long` | 내부 PK | 사용자 식별용 기본 키 |
| `email` | 필수 | `String` | 로그인 ID | LOCAL 로그인과 OAuth 이메일 매핑에 사용, unique |
| `password` | nullable | `String` | 암호화된 비밀번호 | LOCAL 로그인은 필수, OAuth 계정은 없을 수 있음 |
| `name` | 필수 | `String` | 사용자 이름 | 기능 명세 반영 |
| `nickname` | 필수 | `String` | 서비스 표시명 | 화면 표시, 신고/문의 등에서 사용 |
| `phoneNumber` | nullable | `String` | 전화번호 | BUYER는 선택, AGENT는 필수 |
| `role` | 필수 | `Role` | 사용자 권한 | `BUYER`, `AGENT`, `ADMIN` |
| `status` | 필수 | `UserStatus` | 계정 상태 | `ACTIVE`, `INACTIVE`, `BANNED`, `DELETED` |
| `provider` | 필수 | `AuthProvider` | 인증 제공자 | 기본값 `LOCAL`, 추후 `KAKAO`, `NAVER`, `GOOGLE` 확장 |
| `providerId` | nullable | `String` | OAuth 제공자 사용자 ID | OAuth 계정 식별에 사용 |
| `lastLoginAt` | nullable | `LocalDateTime` | 마지막 로그인 시각 | 관리자 회원 관리, 보안 로그에 활용 |
| `createdAt` | 필수 | `LocalDateTime` | 생성 시각 | `BaseTimeEntity`에서 공통 처리 |
| `updatedAt` | 필수 | `LocalDateTime` | 수정 시각 | `BaseTimeEntity`에서 공통 처리 |

## Enum

### Role

| 값 | 설명 |
| --- | --- |
| `BUYER` | 집을 구하거나 관심 매물, 신고, 추천 기능을 사용하는 일반 사용자 |
| `AGENT` | 매물을 등록하고 관리하는 집주인 또는 중개인 |
| `ADMIN` | 회원, 매물, 신고를 관리하는 관리자 |

### UserStatus

| 값 | 설명 |
| --- | --- |
| `ACTIVE` | 정상 이용 가능 |
| `INACTIVE` | 사용자가 비활성화한 계정 |
| `BANNED` | 관리자에 의해 이용 제한된 계정 |
| `DELETED` | 탈퇴 처리된 계정 |

### AuthProvider

| 값 | 설명 |
| --- | --- |
| `LOCAL` | 이메일/비밀번호 기반 자체 로그인 |
| `KAKAO` | 카카오 OAuth 로그인 |
| `NAVER` | 네이버 OAuth 로그인 |
| `GOOGLE` | 구글 OAuth 로그인 |

## 회원가입 필드

| 필드 | BUYER | AGENT | 설명 |
| --- | --- | --- | --- |
| `email` | 필수 | 필수 | 로그인 ID, unique |
| `password` | 필수 | 필수 | LOCAL 회원가입 시 필수 |
| `name` | 필수 | 필수 | 사용자 이름 |
| `nickname` | 필수 | 필수 | 서비스 표시명 |
| `phoneNumber` | 선택 | 필수 | AGENT는 매물 등록 주체이므로 필수 |
| `role` | 필수 | 필수 | 회원가입 시 `BUYER`, `AGENT`만 허용 |

## 설계 메모

- `ADMIN`은 일반 회원가입 API로 생성하지 않는다.
- `role`과 `status`는 의미가 다르므로 API와 검증 로직을 분리한다.
- `BUYER -> AGENT` 역할 변경은 허용한다.
- `AGENT -> BUYER`, `ADMIN` 부여, `BANNED` 처리는 관리자 정책으로 별도 관리한다.
- OAuth 최초 가입 시 `phoneNumber`는 선택으로 두고, 필요한 경우 추가 정보 입력 단계에서 받을 수 있다.
