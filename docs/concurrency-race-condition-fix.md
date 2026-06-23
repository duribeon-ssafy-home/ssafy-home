# 동시성 문제(Race Condition) 발견 및 해결

## 개요

멀티스레드 환경에서 **check-then-act 패턴**이 원자적(atomic)이지 않아 발생하는 경쟁 조건 문제를 분석하고, 실제로 유효한 취약점을 찾아 해결한 기록.

---

## 핵심 개념: Check-Then-Act의 위험성

"확인하고 → 행동하는" 두 단계가 원자적이지 않으면, 두 요청이 동시에 실행될 때 아래와 같은 현상이 발생한다.

```
스레드 A: [존재 확인: 없음] ──────────────► [저장]
스레드 B:        [존재 확인: 없음] ──────────────► [저장]  ← 둘 다 "없음"으로 통과
```

Spring의 `@Transactional` 안에 있어도, 두 SELECT는 서로를 인식하지 못한 채 실행된다.

---

## 분석: 찜하기 / 신고는 실제 문제 없음

처음에는 `FavoriteService`와 `ReportService`도 동일한 check-then-act 구조를 가지고 있어 취약점으로 보였다.

```java
// FavoriteService.java
if (favoriteRepository.existsByUserIdAndPropertyPropertyId(userId, propertyId)) {
    throw new BusinessException(ErrorCode.FAVORITE_ALREADY_EXISTS);
}
favoriteRepository.save(...); // 이론상 race condition 가능 구간
```

그러나 프론트엔드 코드를 확인하니 **UI 레벨에서 이미 방어**하고 있었다.

**찜하기**: `useFavorites.js`의 `toggleFavorite()`이 현재 찜 상태를 확인한 뒤 `addFavorite` 또는 `removeFavorite`를 구분 호출하므로, 동일한 매물에 `addFavorite`가 중복 호출되지 않는다.

```js
// useFavorites.js
async function toggleFavorite(propertyId) {
  if (next.has(propertyId)) {
    await removeFavorite(propertyId)  // 이미 찜 → 해제
  } else {
    await addFavorite(propertyId)     // 미찜 → 추가
  }
}
```

**신고**: `isSubmittingReport` 플래그와 버튼 `disabled` 처리로 제출 중 중복 요청이 불가능하다.

```js
// PropertyDetailView.vue
if (isSubmittingReport.value) return  // 진행 중이면 즉시 반환
isSubmittingReport.value = true
```

```html
<button type="submit" :disabled="isSubmittingReport || !reportReason">
```

**결론**: 두 케이스 모두 일반적인 사용자 플로우에서 race condition이 발생하지 않는다. DB의 UNIQUE 제약이 최후 방어선으로 존재하므로 서버 레벨의 추가 조치는 불필요하다.

---

## 실제 문제: AuthService.refresh() — Refresh Token 이중 발급 (보안 취약점)

### 왜 UI로 막을 수 없는가

Refresh token 재발급은 **만료된 access token을 사용할 때 클라이언트 코드가 자동으로 보내는 요청**이다. 사용자가 여러 탭에서 동시에 API를 호출하면, 각 탭이 독립적으로 refresh 요청을 날린다. UI 레벨의 버튼 비활성화로는 이 상황을 막을 수 없다.

### Refresh Token Rotation이란

Access token이 만료되면 refresh token으로 새 access token을 발급하되, **기존 refresh token은 즉시 무효화**하는 방식. 토큰 탈취 시 피해를 최소화하기 위한 보안 메커니즘이다.

### 문제 코드

```java
// AuthService.java
RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())...;
if (!refreshToken.isValid()) {
    throw new BusinessException(ErrorCode.INVALID_TOKEN);
}
refreshToken.revoke();                              // ← 비원자적 실행
String newAccessToken = jwtTokenProvider.createAccessToken(user);
String newRefreshToken = createRefreshToken(user);
```

### 실제 실행 흐름 (탭 A, 탭 B 동시 요청)

```
[탭 A] findByToken("abc") → RefreshToken (valid=true)  ┐
[탭 B] findByToken("abc") → RefreshToken (valid=true)  ┘  둘 다 같은 토큰 로드

[탭 A] isValid() → true
[탭 B] isValid() → true  ← 둘 다 유효성 통과

[탭 A] revoke() → 무효화 처리
[탭 B] revoke() → 이미 무효화된 토큰을 또 무효화 (예외 없이 통과)

[탭 A] → accessToken-1 발급 ✓
[탭 B] → accessToken-2 발급 ✓  ← 이중 발급 발생
```

**공격 시나리오**: refresh token이 탈취되었을 때, 원래 사용자와 공격자가 동시에 요청을 보내면 둘 다 새 access token을 발급받는다. Rotation의 보안 목적이 무력화된다.

### 해결 방법: 비관적 락(Pessimistic Lock)

`findByToken` 시점에 **`SELECT ... FOR UPDATE`** 를 걸어, 첫 번째 요청의 트랜잭션이 끝날 때까지 두 번째 요청을 DB 레벨에서 블로킹한다.

```java
// RefreshTokenRepository.java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT r FROM RefreshToken r WHERE r.token = :token")
Optional<RefreshToken> findByTokenWithLock(@Param("token") String token);
```

**락 이후 실행 흐름**:

```
[탭 A] SELECT ... FOR UPDATE → "abc" 행에 락 획득
[탭 B] SELECT ... FOR UPDATE → A의 커밋까지 대기 (블로킹)

[탭 A] isValid() → true → revoke → 새 토큰 발급 → 커밋 (락 해제)
[탭 B] 재개 → isValid() → false → 예외 (이미 사용된 토큰)  ← 정상 동작
```

### 낙관적 락 vs 비관적 락 선택 이유

| 구분 | 낙관적 락 (Optimistic) | 비관적 락 (Pessimistic) |
|------|----------------------|----------------------|
| 방식 | 버전 컬럼으로 충돌 감지 후 예외 | 조회 시점에 DB 행 잠금 |
| 충돌 시 | `OptimisticLockException` → 재시도 필요 | 두 번째 요청이 대기 후 순차 실행 |
| 적합한 경우 | 충돌이 드문 경우 | 충돌이 확실한 경우 |

Refresh token 재사용은 **의도적으로 발생 가능한 케이스**(탈취, 네트워크 재전송, 멀티 탭)이므로 비관적 락이 적합하다. 낙관적 락을 쓰면 재시도 시 이미 무효화된 토큰으로 재접근하게 되어 결국 실패한다.

---

## 핵심 교훈

1. **race condition처럼 보이는 코드라도, UI 레벨의 보호 장치까지 함께 분석해야 실제 취약점인지 판단할 수 있다.**
2. **UI로 막을 수 없는 보안 관련 상태 변경에는 DB 레벨의 비관적 락이 필요하다.**
3. **낙관적 락과 비관적 락은 "충돌 빈도"와 "실패 시 재시도 가능 여부"를 기준으로 선택한다.**
