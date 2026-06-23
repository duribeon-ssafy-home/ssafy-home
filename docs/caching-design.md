# 캐싱 도입 설계 보고서

## 1. 문제 정의

### 병목 지점: AI 매물 비교 API

`POST /api/ai/chat` (매물 비교 의도)가 호출될 때 `AiCompareService.compare()`는 다음 순서로 동작한다.

```
요청 수신
  └─ 매물 N개에 대해 순차 실행
       ├─ propertyService.getProperty(id)     → DB 조회
       └─ riskService.analyzeRisk(id)         → DB 조회 2회 (properties + reports)
  └─ LLM 호출 (OpenAI API)
응답 반환
```

매물 4개 비교 시 LLM 호출 전에 **DB 8회가 직렬로 실행**된다. `analyzeRisk()`는 동일한 매물 ID에 대해 항상 동일한 결과를 반환하는 **순수 읽기 연산**임에도, 요청할 때마다 같은 쿼리를 반복 실행하고 있다.

### 왜 `analyzeRisk()`가 캐싱 적합한가

| 조건 | 내용 |
|------|------|
| 순수 읽기 | 외부 상태를 변경하지 않음 |
| 결정론적 | 동일 입력 → 항상 동일 출력 |
| 변경 빈도 낮음 | 신고가 추가되거나 매물 상태가 바뀔 때만 결과가 달라짐 |
| 호출 빈도 높음 | 매물 비교 요청마다 N회 반복 호출 |

---

## 2. 기술 선택: Caffeine vs Redis

### Redis를 선택하지 않은 이유

Redis는 별도 서버 프로세스가 필요하고 네트워크 레이턴시가 발생한다. 분산 환경(서버 여러 대)에서 캐시를 공유해야 할 때 의미 있는 선택이다.

현재 이 서비스는 **단일 인스턴스**로 운영된다. Redis를 도입하면:
- 캐시 조회 시마다 네트워크 왕복 발생
- 별도 인프라 운영 부담
- 오히려 로컬 메모리 조회보다 느려질 수 있음

### Caffeine을 선택한 이유

Caffeine은 JVM 프로세스 내부 메모리에 캐시를 저장한다.

| 항목 | 내용 |
|------|------|
| 조회 속도 | 나노초 수준 (네트워크 없음) |
| TTL 지원 | `expireAfterWrite`로 만료 시간 설정 가능 |
| 최대 크기 제한 | `maximumSize`로 메모리 증가 제어 |
| 의존성 | Spring Boot가 공식 지원 (`spring-boot-starter-cache` + Caffeine) |

---

## 3. 캐시 설정 설계

### TTL (Time To Live): 10분

`@CacheEvict`로 명시적 무효화를 구현하더라도, **코드에서 누락된 무효화 시점**이 생길 수 있다. TTL은 이에 대한 최후 안전망이다.

10분을 선택한 근거:
- 위험도 점수는 신고 접수 후 즉시 반영되어야 하므로 너무 길면 안 됨
- 신고는 자주 발생하지 않으므로 너무 짧으면 캐시 효과가 없음
- `@CacheEvict`가 정상 동작하면 TTL은 거의 발동되지 않음

### 최대 크기: 1,000개

매물 1개당 캐시 항목 1개. 서비스 규모를 고려해 1,000개로 제한하고, 초과 시 LRU 방식으로 오래된 항목을 자동 제거한다.

---

## 4. 캐시 무효화 전략 (Cache Invalidation)

캐싱에서 가장 어려운 문제는 **언제 캐시를 버릴 것인가**이다.

`analyzeRisk(propertyId)`의 결과가 달라지는 시점을 모두 찾아야 한다.

### 무효화가 필요한 시점

| 이벤트 | 위치 | 이유 |
|--------|------|------|
| 신고 접수 | `ReportService.createReport()` | `reportScore` 계산에 신고 목록이 반영됨 |
| 매물 삭제 | `PropertyService.deleteProperty()` | 삭제된 매물의 캐시를 계속 보유할 이유 없음 |

### 처음에 놓치기 쉬운 케이스

`ReportService`만 무효화하면 된다고 생각하기 쉽지만, `PropertyService.deleteProperty()`도 무효화 대상이다. 매물이 삭제된 뒤에도 캐시가 남아 있으면 `PROPERTY_NOT_FOUND`가 발생해야 할 요청에서 캐시된 결과를 그대로 반환할 위험이 있다.

이처럼 **무효화 시점을 빠짐없이 찾는 것**이 캐싱 설계의 핵심 고민이다.

---

## 5. 적용 범위 요약

```
[캐싱 대상]
RiskService.analyzeRisk(Long propertyId)
  └─ @Cacheable(value = "riskScore", key = "#propertyId")

[캐시 무효화]
ReportService.createReport(Long propertyId, ...)
  └─ @CacheEvict(value = "riskScore", key = "#propertyId")

PropertyService.deleteProperty(Long id, ...)
  └─ @CacheEvict(value = "riskScore", key = "#id")
```

---

## 6. 예상 효과

### 캐시 히트 시

| 구간 | 변경 전 | 변경 후 |
|------|---------|---------|
| `analyzeRisk()` 호출 1회 | DB 2회 실행 | 메모리 조회 1회 |
| 4개 매물 비교 시 `analyzeRisk()` | DB 8회 | DB 0회 (캐시 히트 시) |

LLM 호출 전 단계의 DB 부하가 실질적으로 줄어든다.

---

## 7. 면접에서 말할 수 있는 포인트

> "AI 매물 비교 API에서 매물마다 위험도 계산이 반복 실행되는 것을 발견했습니다. 위험도는 신고가 추가되거나 매물이 삭제될 때만 바뀌는 값이라 캐싱 적합성이 높았습니다."

> "Redis 대신 Caffeine을 선택한 이유는 단일 인스턴스 서버에서 네트워크 비용 없이 JVM 메모리에 직접 접근하는 것이 더 빠르기 때문입니다. Redis는 서버가 여러 대로 늘어날 때 필요한 선택입니다."

> "가장 고민한 부분은 캐시 무효화 시점이었습니다. 처음엔 신고 접수 시 evict만 생각했는데, 매물 삭제 시에도 캐시를 제거해야 한다는 걸 흐름을 따라가면서 발견했습니다. `@CacheEvict`가 누락되면 삭제된 매물의 위험도가 그대로 반환될 수 있습니다."

> "TTL을 10분으로 설정한 건 `@CacheEvict`를 보완하는 안전망입니다. 무효화 코드가 누락된 케이스가 생기더라도 최대 10분 뒤에는 자동으로 갱신됩니다."
