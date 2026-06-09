# 위험 분석(Risk Analysis) 코드 가이드

## 목차

1. [전체 구조](#1-전체-구조)
2. [API 진입점 — RiskController](#2-api-진입점--riskcontroller)
3. [타입 정의 — RiskLabel / RiskResponse](#3-타입-정의--risklabel--riskresponse)
4. [데이터 조회 — Repository 확장](#4-데이터-조회--repository-확장)
5. [핵심 로직 — RiskService](#5-핵심-로직--riskservice)
6. [위험 측정 기준 정리](#6-위험-측정-기준-정리)
7. [추가 확장 가능성](#7-추가-확장-가능성)

---

## 1. 전체 구조

요청이 들어오면 아래 순서로 흐릅니다.

```
HTTP GET /api/properties/{propertyId}/risk
        │
        ▼
RiskController.analyzeRisk()
        │
        ▼
RiskService.analyzeRisk()
        ├── PropertyRepository.findById()           → 매물 존재 및 상태 확인
        ├── ReportRepository.findAllByProperty_PropertyId()  → 신고 목록
        └── PropertyRepository.findNearbyDeposits()          → 주변 시세
        │
        ▼
RiskResponse (label, score, 근거 데이터)
```

**설계 원칙:** 매 요청마다 즉시 계산(on-the-fly). DB 조회 2회로 끝냅니다.  
`RiskService`는 `property`, `report` 패키지의 레포지토리에만 의존하는 단방향 구조입니다.

**관련 파일:**

| 역할 | 경로 |
|---|---|
| 컨트롤러 | `risk/controller/RiskController.java` |
| 서비스 | `risk/service/RiskService.java` |
| 응답 DTO | `risk/dto/response/RiskResponse.java` |
| 라벨 타입 | `risk/type/RiskLabel.java` |
| 시세 쿼리 추가 | `property/repository/PropertyRepository.java` |
| 신고 쿼리 추가 | `report/repository/ReportRepository.java` |

---

## 2. API 진입점 — RiskController

```java
@GetMapping("/api/properties/{propertyId}/risk")
public ApiResponse<RiskResponse> analyzeRisk(@PathVariable Long propertyId) {
    return ApiResponse.success("위험 분석 결과를 조회했습니다.", riskService.analyzeRisk(propertyId));
}
```

- **인증 불필요** — SecurityConfig에서 `/api/properties/*/risk`를 `permitAll()` 처리
- 역할은 단순히 서비스 호출 후 `ApiResponse`로 래핑하는 것뿐
- 에러는 `RiskService`에서 `BusinessException`으로 던지고 `GlobalExceptionHandler`가 처리

---

## 3. 타입 정의 — RiskLabel / RiskResponse

### RiskLabel

```java
public enum RiskLabel {
    SAFE, CAUTION, DANGER, UNKNOWN
}
```

`UNKNOWN`은 에러가 아닌 **정상 응답**입니다. "비교 데이터 부족으로 판단 불가" 상태를 명시적으로 표현합니다.

### RiskResponse

```java
public record RiskResponse(
        Long propertyId,
        RiskLabel label,       // 라벨
        int score,             // 총점 (0~100)
        Long marketPriceAvg,   // 주변 평균 시세 (만원), UNKNOWN이면 null
        Double priceGapRate,   // 편차율 (%), UNKNOWN이면 null
        int reportCount,       // 신고 건수
        boolean ownerVerified  // 중개인 직접 등록(AGENT) 여부
) {
    public static RiskResponse unknown(Long propertyId, int reportCount, boolean ownerVerified) {
        return new RiskResponse(propertyId, RiskLabel.UNKNOWN, 0, null, null, reportCount, ownerVerified);
    }
}
```

`UNKNOWN`일 때도 `reportCount`와 `ownerVerified`는 실제 값을 채워서 반환합니다.  
프론트엔드가 "비교 불가지만 신고는 2건 있음" 같은 부분 정보를 활용할 수 있도록 합니다.

---

## 4. 데이터 조회 — Repository 확장

### PropertyRepository — 주변 시세 조회

```java
@Query("""
    SELECT p.deposit FROM Property p
    WHERE p.dong = :dong
      AND p.rentType = :rentType
      AND p.area BETWEEN :areaMin AND :areaMax
      AND p.status = :status
      AND p.propertyId != :excludeId
      AND p.deposit IS NOT NULL
    """)
List<Long> findNearbyDeposits(
        @Param("dong") String dong,
        @Param("rentType") RentType rentType,
        @Param("areaMin") BigDecimal areaMin,
        @Param("areaMax") BigDecimal areaMax,
        @Param("status") PropertyStatus status,
        @Param("excludeId") Long excludeId
);
```

비교 조건 4가지:
- 같은 `dong` (행정동 단위)
- 같은 `rentType` (전세/월세 구분)
- `area ±20㎡` 범위 (면적 유사)
- `status = APPROVED` (승인된 매물만)

`excludeId`로 자기 자신을 제외하고, `deposit IS NOT NULL` 조건으로 가격 없는 행 필터링합니다.

### ReportRepository — 매물별 신고 조회

```java
List<Report> findAllByProperty_PropertyId(Long propertyId);
```

Spring Data JPA의 메서드 이름 규칙으로 자동 쿼리 생성. `property_propertyId` → `JOIN reports r ON r.property_id = ?`

---

## 5. 핵심 로직 — RiskService

### 전체 흐름

```java
public RiskResponse analyzeRisk(Long propertyId) {
    // ① 매물 유효성 검증
    Property property = propertyRepository.findById(propertyId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PROPERTY_NOT_FOUND));
    if (property.getStatus() != PropertyStatus.APPROVED) {
        throw new BusinessException(ErrorCode.PROPERTY_NOT_FOUND);
    }

    // ② 신고 + 소유자 확인 (UNKNOWN에서도 필요하므로 먼저 조회)
    List<Report> reports = reportRepository.findAllByProperty_PropertyId(propertyId);
    boolean ownerVerified = property.getDataSource() == DataSource.AGENT;

    // ③ 필수 필드 없으면 UNKNOWN 조기 반환
    if (property.getArea() == null || property.getDeposit() == null || property.getRentType() == null) {
        return RiskResponse.unknown(propertyId, reports.size(), ownerVerified);
    }

    // ④ 주변 시세 조회 → 3개 미만이면 UNKNOWN
    List<Long> nearbyDeposits = propertyRepository.findNearbyDeposits(...);
    if (nearbyDeposits.size() < NEARBY_MIN_COUNT) {
        return RiskResponse.unknown(propertyId, reports.size(), ownerVerified);
    }

    // ⑤ 점수 계산 → 라벨 판정
    double marketAvg = ...;
    double priceGapRate = (marketAvg - property.getDeposit()) / marketAvg * 100.0;

    int totalScore = Math.min(
        calcPriceScore(priceGapRate) + Math.min(calcReportScore(reports), 40) + ownerPenalty,
        100
    );

    return new RiskResponse(propertyId, determineLabel(totalScore), totalScore, ...);
}
```

### 가격 이상 점수 (0~50점)

```java
private int calcPriceScore(double priceGapRate) {
    if (priceGapRate < 10) return 0;
    if (priceGapRate < 20) return 15;
    if (priceGapRate < 35) return 30;
    return 50;
}
```

`priceGapRate = (평균 - 해당 매물) / 평균 × 100`

평균보다 **저렴할수록** 점수가 올라갑니다. 전세 사기의 핵심 신호가 "시세보다 지나치게 싼 매물"이기 때문입니다. 평균보다 비싸면 음수가 되어 0점을 유지합니다.

### 신고 점수 (0~40점, 초과 시 40점으로 캡)

```java
private int calcReportScore(List<Report> reports) {
    return reports.stream()
            .mapToInt(r -> switch (r.getReason()) {
                case FRAUD_SUSPECTED, FAKE_LISTING -> 15;
                case PRICE_MISMATCH, NO_CONTACT    ->  8;
                case PHOTO_MISMATCH, ETC           ->  4;
            })
            .sum();
}
```

사기 의심도가 높은 사유일수록 가중치를 높게 적용합니다.  
`Math.min(..., 40)`으로 신고만으로 DANGER가 되는 상황을 방지합니다.

### 소유자 미확인 패널티

```java
int ownerPenalty = ownerVerified ? 0 : 10;
```

`data_source = PUBLIC`은 공공 거래 데이터로 등록 중개인 정보가 없습니다.  
소유자 확인 불가 상태임을 10점으로 반영합니다.

### 라벨 판정

```java
private RiskLabel determineLabel(int score) {
    if (score <= 30) return RiskLabel.SAFE;
    if (score <= 60) return RiskLabel.CAUTION;
    return RiskLabel.DANGER;
}
```

---

## 6. 위험 측정 기준 정리

### 점수 구성 (합산 최대 100점)

| 구성 요소 | 최대 점수 | 기준 |
|---|---|---|
| 가격 이상 | 50점 | 주변 평균 대비 편차율 |
| 신고 누적 | 40점 | 사유별 가중치 합산 |
| 소유자 미확인 | 10점 | `data_source = PUBLIC` |

### 가격 편차율 → 점수 매핑

| 편차율 (매물이 평균보다 저렴한 정도) | 점수 |
|---|---|
| 10% 미만 | 0점 |
| 10% 이상 ~ 20% 미만 | 15점 |
| 20% 이상 ~ 35% 미만 | 30점 |
| 35% 이상 | 50점 |
| 평균보다 비쌈 (음수) | 0점 |

### 신고 사유 → 점수 매핑

| 신고 사유 | 건당 점수 |
|---|---|
| `FRAUD_SUSPECTED` (사기 의심), `FAKE_LISTING` (허위 매물) | 15점 |
| `PRICE_MISMATCH` (가격 불일치), `NO_CONTACT` (연락 두절) | 8점 |
| `PHOTO_MISMATCH` (사진 불일치), `ETC` (기타) | 4점 |

### 라벨 기준

| 총점 | 라벨 | 의미 |
|---|---|---|
| 비교 데이터 부족 | `UNKNOWN` | 판단 불가 |
| 0 ~ 30 | `SAFE` | 이상 없음 |
| 31 ~ 60 | `CAUTION` | 주의 필요 |
| 61 ~ 100 | `DANGER` | 위험 매물 |

### UNKNOWN 반환 조건

아래 중 하나라도 해당하면 점수 계산 없이 `UNKNOWN`을 반환합니다.

1. 매물의 `area`, `deposit`, `rentType` 중 하나가 `null`
2. 동일 dong + 동일 rentType + 면적 ±20㎡ 범위 내 비교 매물이 **3개 미만**

---

## 7. 추가 확장 가능성

### 캐싱 (성능 개선)

DB 스키마에 `property_risk_scores` 테이블이 이미 존재합니다.  
지금은 매 요청마다 계산하지만, 트래픽이 증가하면 두 단계로 최적화할 수 있습니다.

**1단계 — Spring Cache로 인메모리 캐싱:**
```java
@Cacheable(value = "riskScore", key = "#propertyId")
public RiskResponse analyzeRisk(Long propertyId) { ... }

// 신고 접수 시 캐시 무효화
@CacheEvict(value = "riskScore", key = "#propertyId")
public void onReportCreated(Long propertyId) { ... }
```

**2단계 — property_risk_scores 테이블 활용:**
```java
// 계산 결과를 DB에 저장 → 다음 조회 시 DB 조회만
PropertyRiskScore saved = riskScoreRepository.save(new PropertyRiskScore(response));
```

### 관리자 수동 라벨 고정

관리자가 특정 매물을 `DANGER`로 수동 지정하면 재계산 시에도 유지하는 기능입니다.  
`property_risk_scores` 테이블에 `is_manual_override BOOLEAN` 컬럼을 추가하고  
서비스에서 해당 플래그를 확인 후 수동 값 우선 반환하면 됩니다.

### 위험 라벨 변경 알림

매물 소유자(`ownerId`)에게 위험 라벨이 `DANGER`로 바뀔 때 알림을 보내는 기능입니다.  
Spring ApplicationEvent를 활용하면 RiskService의 변경 없이 연결할 수 있습니다.

```java
// 이벤트 발행 (RiskService 내부)
eventPublisher.publishEvent(new RiskLabelChangedEvent(propertyId, newLabel));

// 이벤트 수신 (별도 NotificationService)
@EventListener
public void onLabelChanged(RiskLabelChangedEvent event) { ... }
```

### 신고 사유 가중치 동적 조정

현재는 코드에 하드코딩된 가중치를 관리자 설정 테이블로 옮기면  
재배포 없이 가중치를 조정할 수 있습니다.

### 비교 범위 확장

현재는 `dong` 단위로만 비교합니다.  
해당 dong에 매물이 적으면 `gugun` 단위로 폴백하는 로직을 추가해  
`UNKNOWN` 반환 빈도를 줄일 수 있습니다.

```java
// 현재: dong 내 비교
List<Long> nearbyDeposits = propertyRepository.findNearbyDeposits(dong, ...);

// 확장: dong 부족하면 gugun으로 폴백
if (nearbyDeposits.size() < NEARBY_MIN_COUNT) {
    nearbyDeposits = propertyRepository.findNearbyDepositsByGugun(gugun, ...);
}
```
