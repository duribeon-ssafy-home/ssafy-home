# Redis 캐싱 도입 설계 보고서

## 1. 도입 배경

### 문제 발견: AI 매물 비교 API의 반복 연산

`POST /api/ai/chat` (매물 비교)가 호출될 때 내부 동작을 추적하면 다음과 같다.

```
AiOrchestratorService.chat()
  └─ AiCompareService.compare()
       └─ 매물 N개에 대해 순차 실행
            ├─ propertyService.getProperty(id)    → DB 조회 1회
            └─ riskService.analyzeRisk(id)        → DB 조회 2회
                                                    (properties + reports)
       └─ LLM 호출 (수 초 소요)
```

4개 매물 비교 시 LLM 호출 전에만 **DB 12회가 직렬로 실행**된다.

여기서 `riskService.analyzeRisk(id)`에 주목했다. 이 메서드는:
- 해당 매물의 주변 시세 데이터와 신고 목록을 조회해서 위험도를 계산한다
- **신고가 새로 접수되거나 매물이 삭제되지 않는 한 결과가 바뀌지 않는다**
- 같은 매물에 대해 여러 사용자가 비교 요청을 보낼 때마다 동일한 계산이 반복된다

즉, **결과는 같은데 연산은 계속 일어나는** 전형적인 캐싱 적용 대상이다.

---

## 2. 기술 선택: Caffeine → Redis

### 처음 고려한 Caffeine

Spring Boot에서 가장 간단하게 쓸 수 있는 로컬 캐시다. 어노테이션 두 개로 적용 가능하고 별도 인프라가 필요 없다.

그러나 Caffeine은 **JVM 메모리 안에만 캐시를 저장**한다. 이것이 가지는 한계:

```
[Spring Boot 서버]
  └─ JVM Heap
       └─ Caffeine Cache (서버 종료 시 사라짐)
```

- 서버를 재시작할 때마다 캐시가 초기화된다
- 서버가 여러 대가 되면 각 인스턴스가 독립된 캐시를 가져 일관성이 깨진다
- 실무에서 단독으로 쓰이는 경우가 드물다

### Redis를 선택한 이유

Redis는 **서버 프로세스 외부에서 독립적으로 동작하는 인메모리 저장소**다.

```
[Spring Boot 서버 A]  ─┐
[Spring Boot 서버 B]  ─┼─► [Redis] ◄─ 모든 인스턴스가 공유
[Spring Boot 서버 C]  ─┘
```

| 항목 | Caffeine | Redis |
|------|----------|-------|
| 저장 위치 | JVM 힙 메모리 | 별도 프로세스 |
| 앱 재시작 후 | 캐시 초기화 | 캐시 유지 |
| 다중 인스턴스 | 인스턴스마다 별도 캐시 | 공유 캐시 |
| TTL 지원 | 설정 가능 | 기본 지원 |
| 실무 사용 빈도 | 보조적 | 업계 표준 |

현재 이 서비스는 단일 인스턴스로 운영되므로 Caffeine으로도 기술적으로는 충분하다. 그러나 다음 이유로 Redis를 선택했다.

1. **학습 가치**: Redis는 백엔드 개발자에게 사실상 필수 기술이다. 실무 프로젝트에서 직접 적용해보는 경험 자체가 목적이다.
2. **확장 대비**: 서비스가 성장해 서버를 여러 대로 운영하게 될 때 캐시 구조 변경 없이 Redis가 그대로 분산 캐시 역할을 한다.
3. **앱 재시작 내구성**: 배포나 재시작 후에도 캐시가 유지되어 워밍업 없이 바로 캐시 효과를 누릴 수 있다.

---

## 3. 캐시 무효화 전략 (Cache Invalidation)

캐싱의 핵심 과제는 **언제 캐시를 버릴 것인가**다.

`analyzeRisk(propertyId)` 결과에 영향을 주는 이벤트를 모두 찾아야 한다.

### 무효화가 필요한 시점

| 이벤트 | 발생 위치 | 이유 |
|--------|----------|------|
| 신고 접수 | `ReportService.createReport()` | 신고 수가 늘면 위험도 점수가 올라감 |
| 매물 삭제 | `PropertyService.deleteProperty()` | 삭제된 매물의 캐시가 남아있으면 잘못된 결과 반환 |

### 처음 놓치기 쉬운 케이스

`ReportService`만 무효화하면 된다고 생각하기 쉽다. 그러나 `PropertyService.deleteProperty()`도 반드시 필요하다.

매물이 삭제된 후에도 캐시가 살아있으면:
1. 해당 매물에 대해 `analyzeRisk()` 호출 시 캐시 히트 → DB 조회 없이 결과 반환
2. 실제로는 삭제된 매물인데 위험도 분석 결과가 정상적으로 반환됨
3. AI 비교 기능에서 삭제된 매물의 데이터가 혼입될 수 있음

---

## 4. TTL 설계: 10분

`@CacheEvict`로 명시적 무효화를 구현해도 코드에서 누락된 케이스가 생길 수 있다. TTL은 이에 대한 **최후 안전망**이다.

10분을 선택한 근거:
- 신고 접수 후 위험도 갱신이 너무 늦으면 안 되므로 무제한 TTL은 위험
- 신고는 자주 발생하지 않으므로 너무 짧으면 캐시 효과가 없음
- `@CacheEvict`가 정상 동작하면 TTL이 발동되는 경우는 거의 없음

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
| 1 | Caffeine 의존성 제거 → Redis 의존성 추가 (`build.gradle`) | 5분 |
| 2 | Redis 연결 설정 (`application.yml`) | 5분 |
| 3 | `CacheConfig.java` Redis 기반으로 재작성 | 10분 |
| 4 | `RiskService.java` — `@Cacheable` 적용 | 5분 |
| 5 | `ReportService.java` — `@CacheEvict` 적용 | 5분 |
| 6 | `PropertyService.java` — `@CacheEvict` 적용 | 5분 |
| 7 | Docker로 Redis 실행 후 동작 확인 | 10분 |
| **합계** | | **약 45분** |

### 사전 준비 (구현 전 필요)

Redis 실행:
```bash
docker run -d --name redis -p 6379:6379 redis
```

Docker가 없는 경우 Redis 공식 사이트에서 Windows용 설치 가능.

---

## 7. 면접에서 말할 수 있는 포인트

> "AI 매물 비교 API에서 매물별 위험도 계산이 요청마다 반복 실행되는 것을 발견했습니다. 위험도는 신고 접수나 매물 삭제 시에만 바뀌는 값이라 캐싱 적합성이 높았습니다."

> "Caffeine 대신 Redis를 선택한 이유는 두 가지입니다. 첫째, 서버 재시작 후에도 캐시가 유지됩니다. 둘째, 서버를 수평 확장할 때 별도 작업 없이 분산 캐시로 동작합니다."

> "가장 고민한 부분은 무효화 시점이었습니다. 처음엔 신고 접수 시에만 evict하면 된다고 생각했는데, 매물 삭제 시에도 캐시를 제거해야 한다는 걸 발견했습니다. 삭제된 매물의 위험도 캐시가 남아있으면 AI 비교 결과에 잘못된 데이터가 혼입될 수 있기 때문입니다."

> "TTL을 10분으로 설정한 건 `@CacheEvict` 누락 케이스에 대한 안전망입니다."
