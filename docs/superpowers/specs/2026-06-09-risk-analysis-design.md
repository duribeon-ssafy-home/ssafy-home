# 위험 분석(Risk Analysis) 설계

**날짜:** 2026-06-09  
**브랜치:** feature/property  
**담당:** B (정상훈) — `risk` 패키지

---

## 1. 개요

`GET /api/properties/{propertyId}/risk` API를 통해 매물의 전세 사기 위험도를 분석하여 라벨과 점수, 근거 데이터를 반환한다.

위험도는 매 요청마다 즉시 계산(on-the-fly)한다. 성능 이슈 발생 시 `@Cacheable` 또는 `property_risk_scores` 테이블 캐시 레이어를 추가하는 방향으로 확장 가능하다.

---

## 2. 패키지 구조

```
risk/
├── controller/RiskController.java
├── service/RiskService.java
├── dto/response/RiskResponse.java
└── type/RiskLabel.java
```

기존 파일 수정:
- `property/repository/PropertyRepository.java` — 주변 시세 평균 쿼리 추가
- `report/repository/ReportRepository.java` — 매물별 신고 목록 조회 쿼리 추가

---

## 3. API

| 항목 | 내용 |
|---|---|
| 메서드 | GET |
| URL | `/api/properties/{propertyId}/risk` |
| 인증 | 불필요 (공개 조회) |
| 에러 | `PROPERTY_NOT_FOUND` — 존재하지 않거나 APPROVED 상태가 아닌 경우 |

### 응답 예시

```json
{
  "success": true,
  "message": "위험 분석 결과를 조회했습니다.",
  "data": {
    "propertyId": 42,
    "label": "CAUTION",
    "score": 43,
    "marketPriceAvg": 28000,
    "priceGapRate": 28.5,
    "reportCount": 2,
    "ownerVerified": true
  }
}
```

`label = UNKNOWN`일 때 `marketPriceAvg`, `priceGapRate`는 `null`.

---

## 4. RiskResponse DTO

```java
public record RiskResponse(
    Long propertyId,
    RiskLabel label,       // SAFE | CAUTION | DANGER | UNKNOWN
    int score,             // 0~100
    Long marketPriceAvg,   // 주변 평균 시세 (만원), UNKNOWN이면 null
    Double priceGapRate,   // 편차율 (%), UNKNOWN이면 null
    int reportCount,       // 신고 건수
    boolean ownerVerified  // data_source == AGENT이면 true
)
```

---

## 5. 점수 계산 로직

총점 = 가격 이상 점수 + 신고 점수 + 소유자 미확인 패널티 (0~100점)

### 5-1. 가격 이상 점수 (0~50점)

비교 대상 쿼리 조건:
- 같은 `dong`
- 같은 `rent_type`
- `area ±20㎡` 범위
- `status = APPROVED`
- 해당 매물 본인 제외

비교 매물이 **3개 미만**이면 `UNKNOWN` 반환 (데이터 부족).

```
priceGapRate = (평균 deposit - 해당 매물 deposit) / 평균 deposit × 100

priceGapRate < 10%    →  0점
10% ≤ gap < 20%       → 15점
20% ≤ gap < 35%       → 30점
gap ≥ 35%             → 50점
priceGapRate < 0      →  0점 (평균보다 비쌈)
```

### 5-2. 신고 점수 (0~40점, 상한 40점)

| 신고 사유 | 건당 점수 |
|---|---|
| `FRAUD_SUSPECTED`, `FAKE_LISTING` | 15점 |
| `PRICE_MISMATCH`, `NO_CONTACT` | 8점 |
| `PHOTO_MISMATCH`, `ETC` | 4점 |

신고 건수가 많아도 40점을 초과하지 않는다.

### 5-3. 소유자 미확인 패널티

| data_source | 점수 |
|---|---|
| `AGENT` | 0점 |
| `PUBLIC` | +10점 |

---

## 6. 라벨 판정

| 조건 | 라벨 |
|---|---|
| 비교 매물 3개 미만 | `UNKNOWN` |
| 총점 0~30 | `SAFE` |
| 총점 31~60 | `CAUTION` |
| 총점 61~100 | `DANGER` |

---

## 7. 의존 관계

```
RiskController
  └─ RiskService
       ├─ PropertyRepository   (매물 조회 + 주변 시세 평균)
       └─ ReportRepository     (매물별 신고 목록)
```

`RiskService`는 `property`, `report` 패키지의 레포지토리에만 의존한다 (단방향).

---

## 8. 추후 확장 포인트

- `property_risk_scores` 테이블이 스키마에 이미 존재 — 캐시 레이어로 활용 가능
- `RiskService` 내부에 `@Cacheable` 추가로 Redis 캐싱 연결 가능
- 신고 접수(`createReport`) 이벤트 발생 시 캐시 무효화 연결 가능
