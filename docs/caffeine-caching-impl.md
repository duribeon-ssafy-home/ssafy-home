# Caffeine 캐싱 도입 보고서

## 1. 도입 배경

### 문제: AI 매물 비교 API의 반복 연산

`POST /api/ai/chat` (매물 비교)가 호출되면 내부적으로 아래 흐름이 실행된다.

```
AiCompareService.compare()
  └─ 매물 N개에 대해 순차 실행
       ├─ propertyService.getProperty(id)    → DB 조회
       └─ riskService.analyzeRisk(id)        → DB 조회 2회 (properties + reports)
  └─ LLM 호출 (수 초 소요)
```

4개 매물 비교 시 LLM 호출 전에만 DB가 12회 실행된다.

`riskService.analyzeRisk(id)`는 다음 특성을 가진다.

- 동일한 입력에 대해 항상 동일한 결과를 반환한다 (순수 읽기 연산)
- **신고가 새로 접수되거나 매물이 삭제되기 전까지 결과가 변하지 않는다**
- 여러 사용자가 같은 매물을 비교 요청할 때마다 동일한 계산이 반복된다

결과는 같은데 연산이 반복되는 전형적인 캐싱 적용 대상이다.

---

## 2. 기술 선택: Caffeine

### Spring Cache 추상화

Spring은 캐시 구현체와 무관하게 동일한 어노테이션으로 캐싱을 적용할 수 있는 추상화 레이어를 제공한다.

```
@Cacheable / @CacheEvict  ← 비즈니스 코드 (구현체와 무관)
        ↓
  CacheManager            ← 구현체 교체 지점
        ↓
 Caffeine / Redis / EhCache ...
```

즉, Caffeine으로 구현해도 비즈니스 코드는 Redis와 완전히 동일하다. 나중에 Redis로 전환하면 `CacheConfig.java` 설정만 바꾸면 된다.

### Caffeine을 선택한 이유

- 외부 인프라 없이 JVM 메모리 안에서 동작한다 (별도 서버 불필요)
- TTL, 최대 크기 제한 등 실무에서 필요한 기능을 모두 지원한다
- Spring Boot가 공식 지원하는 로컬 캐시 구현체다
- **캐싱의 핵심 개념 학습(무효화 전략, TTL 설계)에 집중할 수 있다**

### Caffeine의 한계 (Redis와 비교)

| 항목 | Caffeine | Redis |
|------|----------|-------|
| 저장 위치 | JVM 힙 메모리 | 별도 프로세스 |
| 앱 재시작 후 캐시 | 초기화됨 | 유지됨 |
| 다중 서버 인스턴스 | 인스턴스마다 별도 캐시 | 공유 캐시 |
| 적합한 환경 | 단일 인스턴스 | 분산 환경 |

현재 서비스는 단일 인스턴스로 운영되므로 Caffeine으로 충분하다. 수평 확장이 필요한 시점에 Redis로 전환하면 된다.

---

## 3. 캐시 설정 설계

### TTL (Time To Live): 10분

`@CacheEvict`로 명시적 무효화를 해도 코드에서 누락된 케이스가 생길 수 있다. TTL은 이에 대한 최후 안전망이다.

- 너무 짧으면 (ex. 1분): 캐시 효과가 거의 없음
- 너무 길면 (ex. 1시간): 신고 접수 후 위험도 갱신이 늦어짐
- **10분**: `@CacheEvict`가 정상 동작하면 거의 발동되지 않으면서, 누락 케이스에 대한 안전망 역할

### 최대 크기: 1,000개

매물 1개당 캐시 항목 1개. 1,000개 초과 시 가장 오래전에 사용된 항목부터 자동 제거(LRU).

---

## 4. 캐시 무효화 전략 (Cache Invalidation)

캐싱에서 가장 어려운 문제는 **언제 캐시를 버릴 것인가**이다.

`analyzeRisk(propertyId)` 결과가 달라지는 시점을 모두 찾아야 한다.

### 무효화 시점

| 이벤트 | 위치 | 이유 |
|--------|------|------|
| 신고 접수 | `ReportService.createReport()` | 신고 수 증가 → 위험도 점수 상승 |
| 매물 삭제 | `PropertyService.deleteProperty()` | 삭제된 매물의 캐시가 남으면 잘못된 결과 반환 |

### 매물 삭제 시 무효화가 필요한 이유

처음에는 신고 접수 시에만 evict하면 된다고 생각하기 쉽다. 그러나 매물 삭제 시에도 반드시 필요하다.

캐시가 남아있는 상태에서 삭제된 매물의 `analyzeRisk()`가 호출되면:

```
캐시 히트 → DB 조회 없이 캐시된 결과 반환
          → 실제로는 삭제된 매물인데 위험도 분석 결과가 정상 반환됨
          → AI 비교 기능에서 삭제된 매물 데이터가 혼입될 수 있음
```

---

## 5. 적용 범위

```
[캐싱 적용]
RiskService.analyzeRisk(Long propertyId)
  └─ @Cacheable(cacheNames = "riskScore", key = "#propertyId")

[캐시 무효화]
ReportService.createReport(Long propertyId, ...)
  └─ @CacheEvict(cacheNames = "riskScore", key = "#propertyId")

PropertyService.deleteProperty(Long id, ...)
  └─ @CacheEvict(cacheNames = "riskScore", key = "#id")
```

---

## 6. 구현 계획 및 예상 소요 시간

| 단계 | 작업 내용 | 예상 시간 |
|------|----------|----------|
| 1 | `build.gradle` — 의존성 확인 (이미 추가됨) | 완료 |
| 2 | `CacheConfig.java` — Caffeine 캐시 매니저 설정 (이미 생성됨) | 완료 |
| 3 | `RiskService.java` — `@Cacheable` 적용 | 5분 |
| 4 | `ReportService.java` — `@CacheEvict` 적용 | 5분 |
| 5 | `PropertyService.java` — `@CacheEvict` 적용 | 5분 |
| 6 | 서버 재시작 후 동작 확인 | 10분 |
| **합계** | | **약 25분** |

---

## 7. 면접에서 말할 수 있는 포인트

> "AI 매물 비교 API에서 위험도 계산이 매 요청마다 반복 실행되는 것을 발견했습니다. 신고 접수나 매물 삭제 시에만 결과가 달라지는 값이라 캐싱 적합성이 높다고 판단했습니다."

> "Spring Cache 추상화를 사용했기 때문에 Caffeine과 Redis 모두 동일한 어노테이션으로 동작합니다. 현재는 단일 인스턴스 환경이라 Caffeine을 선택했고, 수평 확장이 필요해지면 CacheConfig 설정만 바꿔서 Redis로 전환할 수 있습니다."

> "가장 고민한 부분은 무효화 시점이었습니다. 신고 접수 시 evict은 자연스럽게 보이는데, 매물 삭제 시에도 제거해야 한다는 걸 흐름을 따라가다 발견했습니다. 삭제된 매물의 위험도 캐시가 남아있으면 AI 비교 결과에 잘못된 데이터가 혼입될 수 있기 때문입니다."

> "TTL 10분은 @CacheEvict 누락 케이스에 대한 안전망입니다. 명시적 무효화가 항상 완벽하다고 보장할 수 없기 때문에 최대 10분 후에는 자동 갱신되도록 했습니다."
