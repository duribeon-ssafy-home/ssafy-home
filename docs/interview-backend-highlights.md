# 면접 대비: 백엔드 기술 포인트

## 1. Caffeine 캐싱 도입 (구현 예정)

### 왜 이 작업을 선택했나

AI 매물 비교 API(`POST /api/ai/chat`)를 분석하다 아래 흐름을 발견했다.

```
AiCompareService.compare(propertyIds)
  └─ 매물 N개에 대해 반복
       ├─ propertyService.getProperty(id)    → DB 1회
       └─ riskService.analyzeRisk(id)        → DB 2회 (property + reports)
  └─ LLM 호출
```

4개 매물을 비교하면 LLM 호출 이전에 DB가 12번 실행된다.  
`analyzeRisk()`는 **신고가 새로 접수되거나 매물이 삭제되기 전까지 결과가 변하지 않는** 순수 읽기 연산이다.

### 기술 선택: Caffeine

Spring Cache 추상화(`@Cacheable`, `@CacheEvict`)를 사용하면 구현체와 무관하게 동일한 어노테이션으로 캐싱을 적용할 수 있다.

```
@Cacheable / @CacheEvict  ← 비즈니스 코드 (구현체 무관)
        ↓
  CacheManager            ← 구현체 교체 지점
        ↓
 Caffeine ↔ Redis         ← 설정 파일만 교체하면 전환 가능
```

Caffeine을 선택한 이유:
- 외부 인프라 없이 JVM 메모리 내 동작 (별도 서버 불필요)
- 단일 인스턴스 환경에서 Redis와 기능적 차이 없음
- TTL, LRU, 최대 크기 등 실무 캐시 기능 모두 지원

### 설계에서 가장 고민한 부분: 캐시 무효화

캐싱에서 핵심 난제는 **언제 캐시를 버릴 것인가**이다.  
`analyzeRisk(propertyId)` 결과가 달라지는 시점을 모두 찾았다.

| 이벤트 | 위치 | 이유 |
|--------|------|------|
| 신고 접수 | `ReportService.createReport()` | 신고 수 증가 → 위험 점수 상승 |
| 매물 삭제 | `PropertyService.deleteProperty()` | 삭제된 매물의 캐시가 남으면 AI 비교에 잘못된 데이터 혼입 |

매물 삭제 시 무효화가 필요한 이유:

```
캐시 히트 → DB 조회 없이 캐시된 결과 반환
          → 실제로는 삭제된 매물인데 위험도 분석 결과가 정상 반환
          → AI 비교 결과에 삭제된 매물 데이터가 섞임
```

### TTL 설계

`@CacheEvict`로 명시적 무효화를 해도 코드에서 누락된 케이스가 생길 수 있다.  
TTL은 이에 대한 최후 안전망이다.

- 1분: 캐시 효과 거의 없음
- 1시간: 신고 후 위험도 갱신이 지나치게 늦어짐
- **10분**: 명시적 evict이 정상 동작하면 거의 발동되지 않으면서 누락 케이스 커버

### 구현 코드 (설계 기준)

```java
// CacheConfig.java
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("riskScore");
        manager.setCaffeine(
            Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(1000)
        );
        return manager;
    }
}

// RiskService.java
@Cacheable(cacheNames = "riskScore", key = "#propertyId")
public RiskResponse analyzeRisk(Long propertyId) { ... }

// ReportService.java
@CacheEvict(cacheNames = "riskScore", key = "#propertyId")
public void createReport(Long propertyId, ...) { ... }

// PropertyService.java
@CacheEvict(cacheNames = "riskScore", key = "#id")
public void deleteProperty(Long id, ...) { ... }
```

### 면접 스크립트

> "AI 매물 비교 API에서 위험도 계산이 매 요청마다 반복 실행되는 것을 발견했습니다. 같은 매물에 여러 사용자가 비교 요청을 보낼 때마다 동일한 DB 조회가 반복되는 구조였습니다."

> "신고 접수나 매물 삭제 시에만 결과가 달라지는 값이라 캐싱 적합성이 높다고 판단했습니다."

> "Spring Cache 추상화를 사용해서 비즈니스 코드에 `@Cacheable` 어노테이션만 붙이면 됩니다. Caffeine과 Redis 모두 동일한 어노테이션으로 동작하기 때문에, 나중에 수평 확장이 필요해지면 `CacheConfig` 설정만 바꿔서 Redis로 전환할 수 있습니다."

> "가장 고민한 부분은 무효화 시점이었습니다. 신고 접수 시 evict은 자연스럽게 보이는데, 매물 삭제 시에도 반드시 필요하다는 걸 흐름을 따라가다 발견했습니다."

> "TTL 10분은 `@CacheEvict` 누락 케이스에 대한 안전망입니다. 명시적 무효화가 항상 완벽하다고 보장할 수 없기 때문에 최대 10분 후에는 자동 갱신되도록 했습니다."

---

## 2. 동시성 분석 (기술적 판단 경험으로 활용)

### 배경

면접에서 자주 나오는 "동시성 문제를 해결한 경험"을 위해 프로젝트의 동시성 취약 지점을 전수 분석했다.

### 분석 대상

| 서비스 | 취약 패턴 후보 | 실제 보호 여부 |
|--------|--------------|----------------|
| `FavoriteService` | 동시 찜하기 → 중복 등록 | ✅ UI 토글 방식 (찜 → 취소 반복) |
| `ReportService` | 동시 신고 → 중복 신고 | ✅ `isSubmittingReport` 플래그로 버튼 비활성화 |
| `AuthService` | 동시 토큰 갱신 → 경쟁 조건 | ✅ `isRefreshing` + `pendingRequests` 큐로 dedup |

### 결론

"문제가 없다는 것을 검증했다"도 면접 포인트가 된다.

> "동시성 취약 지점을 백엔드부터 HTTP 클라이언트까지 레이어별로 분석했습니다. 처음에는 찜하기, 신고, 토큰 갱신에서 Race Condition 가능성을 발견했습니다."

> "그런데 각 레이어를 깊게 들여다보니 이미 보호 로직이 있었습니다. 찜하기는 토글 방식이라 중복이 불가능하고, 신고는 프론트에서 버튼을 즉시 비활성화합니다. 토큰 갱신은 axios 인터셉터에서 첫 번째 갱신 중에 들어오는 요청을 큐에 담아 완료 후 일괄 처리합니다."

> "없는 문제를 만들기보다 캐싱이라는 실질적인 개선으로 방향을 전환했습니다. 분석 자체는 기존 코드에 대한 이해도를 높이는 의미 있는 과정이었습니다."

---

## 참고 문서

- [`docs/caffeine-caching-impl.md`](caffeine-caching-impl.md) — Caffeine 캐싱 상세 설계
- [`docs/redis-caching-design.md`](redis-caching-design.md) — Redis 캐싱 설계 (향후 전환 시 참고)
- [`docs/concurrency-race-condition-fix.md`](concurrency-race-condition-fix.md) — 동시성 분석 전문
- [`docs/troubleshooting-spring-security-warning.md`](troubleshooting-spring-security-warning.md) — Security 경고 트러블 슈팅
