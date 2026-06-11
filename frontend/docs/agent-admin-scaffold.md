# AGENT/ADMIN 관리 화면 스캐폴딩

## 현재 구현 범위

- 중개인 전용 라우트
  - `/agent/properties`
  - route name: `agent-properties`
  - meta: `{ requiresAuth: true, roles: ['AGENT'] }`
- 관리자 전용 라우트
  - `/admin`
  - route name: `admin-dashboard`
  - meta: `{ requiresAuth: true, roles: ['ADMIN'] }`
  - `/admin/users`
  - route name: `admin-users`
  - meta: `{ requiresAuth: true, roles: ['ADMIN'] }`
  - `/admin/reports`
  - route name: `admin-reports`
  - meta: `{ requiresAuth: true, roles: ['ADMIN'] }`
- 신규 화면
  - `src/views/AgentPropertiesView.vue`
  - `src/views/AdminDashboardView.vue`
  - `src/views/AdminUsersView.vue`
  - `src/views/AdminReportsView.vue`
- 마이페이지 역할 버튼 연결
  - AGENT: `agent-properties`
  - ADMIN: `admin-dashboard`

이번 작업에서는 API를 호출하지 않는다. 목적은 권한 라우트, 진입 버튼, 화면 레이아웃, 빈 상태를 먼저 열어두어 이후 API 연동 작업자가 화면 구조를 재사용할 수 있게 하는 것이다.

## 권한 정책

- 비로그인 사용자는 기존 라우터 가드에 의해 `/login?redirect=...`로 이동한다.
- `BUYER`는 `/agent/properties`, `/admin/**` 접근 시 `/forbidden`으로 이동한다.
- `AGENT`는 `/agent/properties`에 접근할 수 있고 `/admin/**`에는 접근할 수 없다.
- `ADMIN`은 `/admin/**`에 접근할 수 있고 `/agent/properties`에는 접근할 수 없다.

`ADMIN`에게 중개인 화면까지 허용하려면 `/agent/properties`의 `meta.roles`를 `['AGENT', 'ADMIN']`으로 확장하면 된다. 현재는 역할 경계를 명확히 하기 위해 `AGENT`만 허용한다.

## 후속 API 모듈 제안

### 중개인 매물 관리

파일 위치: `src/api/agentPropertyApi.js`

- `getMyProperties(params)` -> `GET /properties/me`
- `createProperty(payload)` -> `POST /properties`
- `updateProperty(propertyId, payload)` -> `PATCH /properties/{propertyId}`
- `deleteProperty(propertyId)` -> `DELETE /properties/{propertyId}`
- `uploadPropertyImages(propertyId, formData)` -> `POST /properties/{propertyId}/images`
- `updatePropertyImage(propertyId, imageId, payload)` -> `PATCH /properties/{propertyId}/images/{imageId}`
- `deletePropertyImage(propertyId, imageId)` -> `DELETE /properties/{propertyId}/images/{imageId}`

연동 위치: `AgentPropertiesView.vue`

- 목록 영역에 로딩, 에러, 빈 상태, 페이지네이션을 추가한다.
- 등록 버튼은 모달 또는 별도 등록 라우트로 확장한다.
- 이미지 관리는 매물 상세 또는 편집 화면에서 업로드, 정렬, 삭제 액션으로 분리한다.

### 관리자 회원 관리

파일 위치: `src/api/adminUserApi.js`

- `getAdminUsers(params)` -> `GET /admin/users`
- `getAdminUser(userId)` -> `GET /admin/users/{userId}`
- `updateAdminUserStatus(userId, payload)` -> `PATCH /admin/users/{userId}/status`
- `updateAdminUserRole(userId, payload)` -> `PATCH /admin/users/{userId}/role`

연동 위치: `AdminUsersView.vue`

- 검색 폼을 활성화하고 `keyword`, `role`, `status`, `page`, `size` 같은 조건을 쿼리로 전달한다.
- 테이블 행 클릭 또는 관리 버튼으로 상세 패널을 연다.
- 상태 변경과 역할 변경은 확인 모달을 거쳐 서버 응답 기준으로 테이블을 갱신한다.

### 관리자 신고 관리

파일 위치: `src/api/adminReportApi.js`

- `getAdminReports(params)` -> `GET /admin/reports`
- `getAdminReport(reportId)` -> `GET /admin/reports/{reportId}`
- `updateAdminReport(reportId, payload)` -> `PATCH /admin/reports/{reportId}`

연동 위치: `AdminReportsView.vue`

- 상태 필터를 활성화하고 `status`, `page`, `size` 조건을 전달한다.
- 신고 상세는 별도 상세 라우트 또는 우측 패널로 확장한다.
- 처리 상태 변경 후에는 현재 목록을 재조회하거나 응답 값으로 해당 행만 갱신한다.

## 응답 처리 기준

기존 프론트 API 패턴처럼 `src/api/axios.js`의 공통 인스턴스를 사용한다. 백엔드 응답이 `ApiResponse<T>` 형태라면 API 함수에서는 `response.data.data`만 반환한다.

```js
import api from '@/api/axios'

export async function getAdminUsers(params) {
  const response = await api.get('/admin/users', { params })
  return response.data.data
}
```

페이지 응답이 내려오는 API는 화면에서 다음 구조를 기준으로 처리한다.

- `content`: 실제 목록 데이터
- `number`: 현재 페이지
- `size`: 페이지 크기
- `totalPages`: 전체 페이지 수
- `totalElements`: 전체 요소 수

백엔드 DTO 필드는 실제 컨트롤러와 응답 DTO를 확인한 뒤 화면 모델을 맞춘다. 프론트에서 임의 필드를 만들지 않는다.

## 화면 상태 확장 규칙

각 화면은 아래 상태를 같은 순서로 처리한다.

1. 초기 로딩
2. API 에러
3. 빈 목록
4. 데이터 목록
5. 페이지네이션 또는 다음 액션

이번 스캐폴딩의 빈 테이블과 기능 카드 영역은 3번과 4번 사이를 연결하기 위한 자리다. 후속 작업에서는 기존 텍스트를 제거하기보다 로딩/에러/데이터 상태에 맞춰 조건부 렌더링을 붙이면 된다.
