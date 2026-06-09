# Risk Analysis Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `GET /api/properties/{propertyId}/risk` API를 구현해 매물의 전세 사기 위험 라벨(SAFE/CAUTION/DANGER/UNKNOWN), 점수(0~100), 근거 데이터를 반환한다.

**Architecture:** 요청마다 즉시 계산(on-the-fly). `RiskService`가 `PropertyRepository`(주변 시세)와 `ReportRepository`(신고 목록)에만 의존하는 단방향 구조. DB의 `property_risk_scores` 테이블은 향후 캐시 레이어로 활용 예정이며 이번 구현에서는 사용하지 않는다.

**Tech Stack:** Spring Boot 3.3.5, Java 17, Spring Data JPA, Spring Security, H2(테스트), MockMvc

---

## 파일 구조

| 작업 | 경로 |
|---|---|
| 생성 | `backend/src/main/java/com/ssafy/home/risk/type/RiskLabel.java` |
| 생성 | `backend/src/main/java/com/ssafy/home/risk/dto/response/RiskResponse.java` |
| 생성 | `backend/src/main/java/com/ssafy/home/risk/service/RiskService.java` |
| 생성 | `backend/src/main/java/com/ssafy/home/risk/controller/RiskController.java` |
| 수정 | `backend/src/main/java/com/ssafy/home/property/repository/PropertyRepository.java` |
| 수정 | `backend/src/main/java/com/ssafy/home/report/repository/ReportRepository.java` |
| 수정 | `backend/src/main/java/com/ssafy/home/common/config/SecurityConfig.java` |
| 생성 | `backend/src/test/java/com/ssafy/home/risk/RiskApiTests.java` |

---

## Task 1: 타입 정의 (RiskLabel + RiskResponse)

**Files:**
- Create: `backend/src/main/java/com/ssafy/home/risk/type/RiskLabel.java`
- Create: `backend/src/main/java/com/ssafy/home/risk/dto/response/RiskResponse.java`

- [ ] **Step 1: RiskLabel enum 생성**

```java
// backend/src/main/java/com/ssafy/home/risk/type/RiskLabel.java
package com.ssafy.home.risk.type;

public enum RiskLabel {
    SAFE, CAUTION, DANGER, UNKNOWN
}
```

- [ ] **Step 2: RiskResponse record 생성**

```java
// backend/src/main/java/com/ssafy/home/risk/dto/response/RiskResponse.java
package com.ssafy.home.risk.dto.response;

import com.ssafy.home.risk.type.RiskLabel;

public record RiskResponse(
        Long propertyId,
        RiskLabel label,
        int score,
        Long marketPriceAvg,
        Double priceGapRate,
        int reportCount,
        boolean ownerVerified
) {
    static RiskResponse unknown(Long propertyId, int reportCount, boolean ownerVerified) {
        return new RiskResponse(propertyId, RiskLabel.UNKNOWN, 0, null, null, reportCount, ownerVerified);
    }
}
```

- [ ] **Step 3: 커밋**

```bash
git add backend/src/main/java/com/ssafy/home/risk/
git commit -m "feat: RiskLabel enum, RiskResponse DTO 추가"
```

---

## Task 2: Repository 쿼리 + SecurityConfig 수정

**Files:**
- Modify: `backend/src/main/java/com/ssafy/home/property/repository/PropertyRepository.java`
- Modify: `backend/src/main/java/com/ssafy/home/report/repository/ReportRepository.java`
- Modify: `backend/src/main/java/com/ssafy/home/common/config/SecurityConfig.java`

- [ ] **Step 1: PropertyRepository에 주변 시세 조회 쿼리 추가**

```java
// PropertyRepository.java 에 아래 import 추가
import com.ssafy.home.property.entity.RentType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;

// 인터페이스 바디에 아래 메서드 추가
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

- [ ] **Step 2: ReportRepository에 매물별 신고 조회 추가**

```java
// ReportRepository.java 인터페이스 바디에 추가
List<Report> findAllByProperty_PropertyId(Long propertyId);
```

- [ ] **Step 3: SecurityConfig에 risk 엔드포인트 퍼밋 추가**

`SecurityConfig.java`의 기존 라인을 수정한다:

```java
// 변경 전
.requestMatchers(HttpMethod.GET, "/api/properties", "/api/properties/*").permitAll()

// 변경 후
.requestMatchers(HttpMethod.GET,
        "/api/properties",
        "/api/properties/*",
        "/api/properties/*/risk").permitAll()
```

- [ ] **Step 4: 커밋**

```bash
git add backend/src/main/java/com/ssafy/home/property/repository/PropertyRepository.java
git add backend/src/main/java/com/ssafy/home/report/repository/ReportRepository.java
git add backend/src/main/java/com/ssafy/home/common/config/SecurityConfig.java
git commit -m "feat: 위험 분석용 Repository 쿼리 추가, SecurityConfig risk 엔드포인트 허용"
```

---

## Task 3: 스켈레톤 생성 + 실패하는 테스트 작성

**Files:**
- Create: `backend/src/main/java/com/ssafy/home/risk/service/RiskService.java`
- Create: `backend/src/main/java/com/ssafy/home/risk/controller/RiskController.java`
- Create: `backend/src/test/java/com/ssafy/home/risk/RiskApiTests.java`

- [ ] **Step 1: RiskService 스켈레톤 생성 (로직 없음)**

```java
// backend/src/main/java/com/ssafy/home/risk/service/RiskService.java
package com.ssafy.home.risk.service;

import com.ssafy.home.property.repository.PropertyRepository;
import com.ssafy.home.report.repository.ReportRepository;
import com.ssafy.home.risk.dto.response.RiskResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RiskService {

    private final PropertyRepository propertyRepository;
    private final ReportRepository reportRepository;

    public RiskResponse analyzeRisk(Long propertyId) {
        throw new UnsupportedOperationException("not implemented");
    }
}
```

- [ ] **Step 2: RiskController 스켈레톤 생성**

```java
// backend/src/main/java/com/ssafy/home/risk/controller/RiskController.java
package com.ssafy.home.risk.controller;

import com.ssafy.home.common.response.ApiResponse;
import com.ssafy.home.risk.dto.response.RiskResponse;
import com.ssafy.home.risk.service.RiskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "위험 분석", description = "매물 전세 사기 위험 분석 API")
@RequiredArgsConstructor
@RestController
public class RiskController {

    private final RiskService riskService;

    @Operation(summary = "매물 위험 분석 조회",
               description = "매물의 전세 사기 위험 라벨, 점수, 근거 데이터를 반환합니다. 인증 없이 공개 조회 가능합니다.")
    @GetMapping("/api/properties/{propertyId}/risk")
    public ApiResponse<RiskResponse> analyzeRisk(@PathVariable Long propertyId) {
        return ApiResponse.success("위험 분석 결과를 조회했습니다.", riskService.analyzeRisk(propertyId));
    }
}
```

- [ ] **Step 3: RiskApiTests 작성**

```java
// backend/src/test/java/com/ssafy/home/risk/RiskApiTests.java
package com.ssafy.home.risk;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssafy.home.auth.jwt.JwtTokenProvider;
import com.ssafy.home.auth.refresh.RefreshTokenRepository;
import com.ssafy.home.property.entity.DataSource;
import com.ssafy.home.property.entity.Property;
import com.ssafy.home.property.entity.PropertyStatus;
import com.ssafy.home.property.entity.RentType;
import com.ssafy.home.property.entity.RoomType;
import com.ssafy.home.property.repository.PropertyRepository;
import com.ssafy.home.report.entity.Report;
import com.ssafy.home.report.repository.ReportRepository;
import com.ssafy.home.report.type.ReportReason;
import com.ssafy.home.user.entity.User;
import com.ssafy.home.user.repository.UserRepository;
import com.ssafy.home.user.type.Role;
import com.ssafy.home.user.type.UserStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:ssafy_home_risk;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class RiskApiTests {

    @Autowired MockMvc mockMvc;
    @Autowired PropertyRepository propertyRepository;
    @Autowired ReportRepository reportRepository;
    @Autowired UserRepository userRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        reportRepository.deleteAll();
        propertyRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void nonExistentPropertyReturns404() throws Exception {
        mockMvc.perform(get("/api/properties/{id}/risk", 9999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PROPERTY_NOT_FOUND"));
    }

    @Test
    void nonApprovedPropertyReturns404() throws Exception {
        Property pending = saveProperty(DataSource.AGENT, PropertyStatus.PENDING,
                "Yeoksam", RentType.JEONSE, BigDecimal.valueOf(50), 20000L);

        mockMvc.perform(get("/api/properties/{id}/risk", pending.getPropertyId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PROPERTY_NOT_FOUND"));
    }

    @Test
    void propertyWithFewerThan3NearbyComparablesReturnsUnknown() throws Exception {
        // 비교 매물 2개만 존재 → UNKNOWN
        saveProperty(DataSource.AGENT, PropertyStatus.APPROVED, "Yeoksam", RentType.JEONSE, BigDecimal.valueOf(50), 30000L);
        saveProperty(DataSource.AGENT, PropertyStatus.APPROVED, "Yeoksam", RentType.JEONSE, BigDecimal.valueOf(50), 30000L);
        Property target = saveProperty(DataSource.AGENT, PropertyStatus.APPROVED,
                "Yeoksam", RentType.JEONSE, BigDecimal.valueOf(50), 28000L);

        mockMvc.perform(get("/api/properties/{id}/risk", target.getPropertyId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.label").value("UNKNOWN"))
                .andExpect(jsonPath("$.data.score").value(0))
                .andExpect(jsonPath("$.data.marketPriceAvg").doesNotExist())
                .andExpect(jsonPath("$.data.priceGapRate").doesNotExist())
                .andExpect(jsonPath("$.data.ownerVerified").value(true))
                .andExpect(jsonPath("$.data.reportCount").value(0));
    }

    @Test
    void agentPropertyWithNormalPriceAndNoReportsReturnsSafe() throws Exception {
        // 비교 5개: 평균 30000, 대상 28000 → gap 6.7% → 0점, 신고 없음, AGENT → SAFE(0점)
        for (int i = 0; i < 5; i++) {
            saveProperty(DataSource.AGENT, PropertyStatus.APPROVED, "Yeoksam", RentType.JEONSE, BigDecimal.valueOf(50), 30000L);
        }
        Property target = saveProperty(DataSource.AGENT, PropertyStatus.APPROVED,
                "Yeoksam", RentType.JEONSE, BigDecimal.valueOf(50), 28000L);

        mockMvc.perform(get("/api/properties/{id}/risk", target.getPropertyId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.label").value("SAFE"))
                .andExpect(jsonPath("$.data.score").value(0))
                .andExpect(jsonPath("$.data.marketPriceAvg").value(30000))
                .andExpect(jsonPath("$.data.ownerVerified").value(true))
                .andExpect(jsonPath("$.data.reportCount").value(0));
    }

    @Test
    void agentPropertyWithModeratePriceGapAndReportReturnsCaution() throws Exception {
        // 비교 5개: 평균 30000, 대상 22000 → gap 26.7% → 30점
        // NO_CONTACT 신고 1건 → 8점 / 합계 38점 → CAUTION
        User buyer = saveUser("buyer@test.com", Role.BUYER);
        for (int i = 0; i < 5; i++) {
            saveProperty(DataSource.AGENT, PropertyStatus.APPROVED, "Yeoksam", RentType.JEONSE, BigDecimal.valueOf(50), 30000L);
        }
        Property target = saveProperty(DataSource.AGENT, PropertyStatus.APPROVED,
                "Yeoksam", RentType.JEONSE, BigDecimal.valueOf(50), 22000L);
        saveReport(buyer, target, ReportReason.NO_CONTACT);

        mockMvc.perform(get("/api/properties/{id}/risk", target.getPropertyId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.label").value("CAUTION"))
                .andExpect(jsonPath("$.data.score").value(38))
                .andExpect(jsonPath("$.data.reportCount").value(1))
                .andExpect(jsonPath("$.data.ownerVerified").value(true));
    }

    @Test
    void publicPropertyWithLargePriceGapAndFraudReportReturnsDanger() throws Exception {
        // 비교 5개: 평균 30000, 대상(PUBLIC) 15000 → gap 50% → 50점
        // FRAUD_SUSPECTED 1건 → 15점 / PUBLIC 패널티 10점 / 합계 75점 → DANGER
        User buyer = saveUser("buyer@test.com", Role.BUYER);
        for (int i = 0; i < 5; i++) {
            saveProperty(DataSource.AGENT, PropertyStatus.APPROVED, "Yeoksam", RentType.JEONSE, BigDecimal.valueOf(50), 30000L);
        }
        Property target = saveProperty(DataSource.PUBLIC, PropertyStatus.APPROVED,
                "Yeoksam", RentType.JEONSE, BigDecimal.valueOf(50), 15000L);
        saveReport(buyer, target, ReportReason.FRAUD_SUSPECTED);

        mockMvc.perform(get("/api/properties/{id}/risk", target.getPropertyId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.label").value("DANGER"))
                .andExpect(jsonPath("$.data.score").value(75))
                .andExpect(jsonPath("$.data.reportCount").value(1))
                .andExpect(jsonPath("$.data.ownerVerified").value(false));
    }

    @Test
    void riskApiIsPublicAndRequiresNoAuthentication() throws Exception {
        Property property = saveProperty(DataSource.AGENT, PropertyStatus.APPROVED,
                "Yeoksam", RentType.JEONSE, BigDecimal.valueOf(50), 20000L);

        // 토큰 없이 호출해도 200 반환 (404가 아닌 UNKNOWN 이어야 함)
        mockMvc.perform(get("/api/properties/{id}/risk", property.getPropertyId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.label").value("UNKNOWN"));
    }

    // ─────────────────────────────────────────────
    // 헬퍼
    // ─────────────────────────────────────────────

    private Property saveProperty(DataSource dataSource, PropertyStatus status,
                                   String dong, RentType rentType,
                                   BigDecimal area, Long deposit) {
        return propertyRepository.save(Property.builder()
                .ownerId(null)   // RiskService는 ownerId가 아닌 dataSource로 ownerVerified 판단
                .title("Test Property")
                .address("Seoul Gangnam")
                .sido("Seoul")
                .gugun("Gangnam")
                .dong(dong)
                .rentType(rentType)
                .roomType(RoomType.ONE_ROOM)
                .area(area)
                .deposit(deposit)
                .dataSource(dataSource)
                .status(status)
                .build());
    }

    private User saveUser(String email, Role role) {
        return userRepository.save(User.builder()
                .email(email)
                .password("encoded")
                .name(role.name())
                .nickname(email.split("@")[0])
                .role(role)
                .status(UserStatus.ACTIVE)
                .build());
    }

    private Report saveReport(User user, Property property, ReportReason reason) {
        return reportRepository.save(Report.builder()
                .user(user)
                .property(property)
                .reason(reason)
                .content("test")
                .build());
    }
}
```

- [ ] **Step 4: 빌드 및 테스트 실패 확인**

```powershell
.\gradlew.bat test --tests "com.ssafy.home.risk.RiskApiTests"
```

예상 결과: `UnsupportedOperationException` 등으로 테스트 실패. 컴파일은 성공해야 한다.

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/ssafy/home/risk/
git add backend/src/test/java/com/ssafy/home/risk/
git commit -m "test: RiskApiTests 작성 (실패), RiskService/Controller 스켈레톤 추가"
```

---

## Task 4: RiskService 로직 구현 → 테스트 통과

**Files:**
- Modify: `backend/src/main/java/com/ssafy/home/risk/service/RiskService.java`

- [ ] **Step 1: RiskService 전체 구현**

```java
// backend/src/main/java/com/ssafy/home/risk/service/RiskService.java
package com.ssafy.home.risk.service;

import com.ssafy.home.common.exception.BusinessException;
import com.ssafy.home.common.exception.ErrorCode;
import com.ssafy.home.property.entity.DataSource;
import com.ssafy.home.property.entity.Property;
import com.ssafy.home.property.entity.PropertyStatus;
import com.ssafy.home.property.repository.PropertyRepository;
import com.ssafy.home.report.entity.Report;
import com.ssafy.home.report.repository.ReportRepository;
import com.ssafy.home.risk.dto.response.RiskResponse;
import com.ssafy.home.risk.type.RiskLabel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RiskService {

    private static final int NEARBY_MIN_COUNT = 3;

    private final PropertyRepository propertyRepository;
    private final ReportRepository reportRepository;

    public RiskResponse analyzeRisk(Long propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROPERTY_NOT_FOUND));

        if (property.getStatus() != PropertyStatus.APPROVED) {
            throw new BusinessException(ErrorCode.PROPERTY_NOT_FOUND);
        }

        List<Report> reports = reportRepository.findAllByProperty_PropertyId(propertyId);
        boolean ownerVerified = property.getDataSource() == DataSource.AGENT;

        if (property.getArea() == null || property.getDeposit() == null || property.getRentType() == null) {
            return RiskResponse.unknown(propertyId, reports.size(), ownerVerified);
        }

        BigDecimal areaMin = property.getArea().subtract(BigDecimal.valueOf(20));
        BigDecimal areaMax = property.getArea().add(BigDecimal.valueOf(20));

        List<Long> nearbyDeposits = propertyRepository.findNearbyDeposits(
                property.getDong(), property.getRentType(),
                areaMin, areaMax, PropertyStatus.APPROVED, propertyId);

        if (nearbyDeposits.size() < NEARBY_MIN_COUNT) {
            return RiskResponse.unknown(propertyId, reports.size(), ownerVerified);
        }

        double marketAvg = nearbyDeposits.stream().mapToLong(Long::longValue).average().orElseThrow();
        double priceGapRate = (marketAvg - property.getDeposit()) / marketAvg * 100.0;

        int priceScore = calcPriceScore(priceGapRate);
        int reportScore = Math.min(calcReportScore(reports), 40);
        int ownerPenalty = ownerVerified ? 0 : 10;
        int totalScore = Math.min(priceScore + reportScore + ownerPenalty, 100);

        return new RiskResponse(
                propertyId,
                determineLabel(totalScore),
                totalScore,
                Math.round(marketAvg),
                Math.round(priceGapRate * 10.0) / 10.0,
                reports.size(),
                ownerVerified
        );
    }

    private int calcPriceScore(double priceGapRate) {
        if (priceGapRate < 10) return 0;
        if (priceGapRate < 20) return 15;
        if (priceGapRate < 35) return 30;
        return 50;
    }

    private int calcReportScore(List<Report> reports) {
        return reports.stream()
                .mapToInt(r -> switch (r.getReason()) {
                    case FRAUD_SUSPECTED, FAKE_LISTING -> 15;
                    case PRICE_MISMATCH, NO_CONTACT -> 8;
                    case PHOTO_MISMATCH, ETC -> 4;
                })
                .sum();
    }

    private RiskLabel determineLabel(int score) {
        if (score <= 30) return RiskLabel.SAFE;
        if (score <= 60) return RiskLabel.CAUTION;
        return RiskLabel.DANGER;
    }
}
```

- [ ] **Step 2: 테스트 실행 — 전체 통과 확인**

```powershell
.\gradlew.bat test --tests "com.ssafy.home.risk.RiskApiTests"
```

예상 출력:
```
RiskApiTests > nonExistentPropertyReturns404() PASSED
RiskApiTests > nonApprovedPropertyReturns404() PASSED
RiskApiTests > propertyWithFewerThan3NearbyComparablesReturnsUnknown() PASSED
RiskApiTests > agentPropertyWithNormalPriceAndNoReportsReturnsSafe() PASSED
RiskApiTests > agentPropertyWithModeratePriceGapAndReportReturnsCaution() PASSED
RiskApiTests > publicPropertyWithLargePriceGapAndFraudReportReturnsDanger() PASSED
RiskApiTests > riskApiIsPublicAndRequiresNoAuthentication() PASSED
```

- [ ] **Step 3: 전체 테스트 회귀 확인**

```powershell
.\gradlew.bat test
```

예상: BUILD SUCCESSFUL, 기존 테스트 포함 전체 통과.

- [ ] **Step 4: 최종 커밋**

```bash
git add backend/src/main/java/com/ssafy/home/risk/service/RiskService.java
git commit -m "feat: GET /api/properties/{id}/risk 위험 분석 API 구현"
```
