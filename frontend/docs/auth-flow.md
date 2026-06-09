# 인증 흐름

## 회원가입 요청 흐름

회원가입 화면은 사용자가 입력한 `name`, `nickname`, `email`, `password`, `phoneNumber`, `role` 값을 `POST /api/auth/signup`으로 전송한다. 백엔드는 공통 응답 형식인 `{ success, message, data, errorCode }`로 응답하며, 성공 시 `data`에는 가입된 사용자 정보가 들어온다. `role`은 `BUYER` 또는 `AGENT`를 사용하고, `AGENT` 가입에는 전화번호가 필요하다.

## 회원가입 성공 후 자동 로그인 흐름

회원가입 API는 토큰을 반환하지 않는다. 그래서 프론트는 회원가입이 성공하면 같은 `email`과 `password`로 `POST /api/auth/login`을 한 번 더 호출한다. 자동 로그인까지 성공하면 로그인 응답의 토큰과 사용자 정보를 저장하고, 기존에 보호 페이지에서 넘어온 경우 `redirect` 경로로 이동한다.

## 로그인 성공 후 토큰, user, role 저장 흐름

로그인 응답의 `data`에는 `accessToken`, `refreshToken`, `tokenType`, `expiresIn`, `user`가 포함된다. auth store는 `accessToken`, `refreshToken`, `user`를 보관하고, `role`은 `user.role`에서 계산한다. 새로고침 후 상태 복구를 위해 현재 구현은 localStorage를 사용한다. localStorage 기반 토큰 저장은 XSS 방어가 중요하므로 운영 환경에서는 토큰 만료 정책, CSP, 입력값 방어, 또는 httpOnly cookie 전략을 함께 검토해야 한다.

## 백엔드 토큰 유효기간과 사용자 경험

백엔드 설정 기준 access token 유효기간은 `3600`초, 즉 1시간이다. refresh token 유효기간은 `1209600`초, 즉 14일이다. 로그인 직후 사용자는 access token으로 인증 API를 이용하고, access token이 만료되는 1시간이 지나도 화면에서 즉시 로그아웃되지는 않는다.

프론트는 만료 시각을 미리 계산해 선제 갱신하지 않고, 보호 API 요청에서 백엔드가 401을 반환하는 순간 refresh token으로 재발급을 시도한다. 예를 들어 로그인 후 1시간이 지난 뒤 찜 목록, 추천, 신고 같은 인증 API를 호출하면 첫 요청은 401을 받을 수 있고, 이때 프론트가 `POST /api/auth/refresh`를 호출한다. refresh token이 아직 14일 이내이고 서버에서 유효하면 새 access token과 refresh token을 저장한 뒤 사용자가 누른 원래 요청을 자동으로 한 번 더 실행한다. 이 과정은 성공하면 사용자가 별도 조작 없이 이어서 사용하게 된다.

refresh token이 만료되었거나 로그아웃으로 무효화되었거나 서버에서 유효하지 않다고 판단하면 자동 갱신이 실패한다. 이때 프론트는 저장된 인증 정보를 삭제하고 로그인 페이지로 이동시킨다. 사용자 입장에서는 마지막 로그인 이후 최대 14일 동안은 자동 갱신으로 세션이 이어질 수 있고, 14일이 지나거나 refresh token이 무효화된 뒤에는 다시 로그인해야 한다.

## 라우터 guard 동작 방식

라우터 guard는 최초 진입 시 저장된 access token이 있으면 `GET /api/auth/me`로 사용자 정보를 복구한다. `meta.requiresAuth`가 있는 페이지는 인증 상태가 없으면 로그인 페이지로 보내고, 원래 가려던 경로는 `redirect` query에 담는다. `meta.roles`가 있으면 현재 `user.role`이 허용 목록에 포함되는지 확인하고, 권한이 없으면 권한 없음 화면으로 이동한다.

## 401 발생 시 자동 갱신 방식

API 요청에서 401이 발생하면 Axios 응답 인터셉터가 refresh token을 확인한다. refresh token이 있으면 `POST /api/auth/refresh`를 호출해 새 access token과 refresh token을 발급받고, 저장소와 auth store를 갱신한 뒤 실패했던 원래 요청을 한 번 재시도한다. refresh token 갱신에 실패하면 클라이언트 세션을 비우고 로그인 페이지로 이동한다.

## 403 처리 방식

403은 인증은 되었지만 권한이 부족한 상태로 본다. 로그인, 회원가입 같은 인증 요청에서 발생한 403은 화면에서 에러 메시지로 보여주고, 일반 API 요청이나 role guard에서 발생한 403은 `/forbidden` 안내 화면으로 이동시킨다.
