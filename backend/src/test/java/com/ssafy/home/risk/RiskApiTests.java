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

        // 토큰 없이 호출해도 200 반환 (비교 매물 없어서 UNKNOWN)
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
                .ownerId(null)
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
