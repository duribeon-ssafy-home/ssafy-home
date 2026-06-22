package com.ssafy.home.ai;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssafy.home.auth.jwt.JwtTokenProvider;
import com.ssafy.home.auth.refresh.RefreshTokenRepository;
import com.ssafy.home.lifestyle.entity.LifestyleResult;
import com.ssafy.home.lifestyle.repository.LifestyleResultRepository;
import com.ssafy.home.lifestyle.type.LifestyleType;
import com.ssafy.home.property.entity.DataSource;
import com.ssafy.home.property.entity.Property;
import com.ssafy.home.property.entity.PropertyStatus;
import com.ssafy.home.property.entity.RentType;
import com.ssafy.home.property.entity.RoomType;
import com.ssafy.home.property.repository.PropertyRepository;
import com.ssafy.home.report.repository.ReportRepository;
import com.ssafy.home.user.entity.User;
import com.ssafy.home.user.repository.UserRepository;
import com.ssafy.home.user.type.Role;
import com.ssafy.home.user.type.UserStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:ssafy_home_ai_compare;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class AiCompareApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private LifestyleResultRepository lifestyleResultRepository;

    @BeforeEach
    void setUp() {
        reportRepository.deleteAll();
        propertyRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        lifestyleResultRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("AI 비교 분석은 선택된 매물과 최신 생활유형을 근거로 응답한다")
    void compareSelectedProperties() throws Exception {
        User user = saveUser();
        saveLifestyleResult(user.getId());
        Property first = saveProperty("First Property", 1000L, 45, BigDecimal.valueOf(26), 2020);
        Property second = saveProperty("Second Property", 700L, 35, BigDecimal.valueOf(22), 2012);

        mockMvc.perform(post("/api/ai/compare")
                        .header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "propertyIds": [%d, %d],
                                  "question": "내 생활패턴 기준으로 어디가 제일 좋아?"
                                }
                                """.formatted(first.getPropertyId(), second.getPropertyId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.recommendedPropertyId").exists())
                .andExpect(jsonPath("$.data.lifestyleAvailable").value(true))
                .andExpect(jsonPath("$.data.requiresLifestyleSurvey").value(false))
                .andExpect(jsonPath("$.data.propertyAnalyses.length()").value(2))
                .andExpect(jsonPath("$.data.sources[0].type").value("LIFESTYLE"));
    }

    @Test
    @DisplayName("생활유형이 없어도 AI 비교 분석은 실패하지 않고 설문을 안내한다")
    void compareWithoutLifestyleGuidesSurvey() throws Exception {
        User user = saveUser();
        Property first = saveProperty("First Property", 1000L, 45, BigDecimal.valueOf(26), 2020);
        Property second = saveProperty("Second Property", 700L, 35, BigDecimal.valueOf(22), 2012);

        mockMvc.perform(post("/api/ai/compare")
                        .header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "propertyIds": [%d, %d],
                                  "question": "내 생활패턴 기준으로 어디가 제일 좋아?"
                                }
                                """.formatted(first.getPropertyId(), second.getPropertyId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.lifestyleAvailable").value(false))
                .andExpect(jsonPath("$.data.requiresLifestyleSurvey").value(true))
                .andExpect(jsonPath("$.data.surveyGuideMessage").value("생활유형 설문을 완료하면 생활패턴 기준 추천을 더 정확하게 받을 수 있어요."))
                .andExpect(jsonPath("$.data.propertyAnalyses[0].lifestyleFitScore").doesNotExist());
    }

    @Test
    @DisplayName("AI 비교 분석은 중복 매물 ID를 거부한다")
    void duplicatePropertyIdsAreRejected() throws Exception {
        User user = saveUser();
        saveLifestyleResult(user.getId());
        Property property = saveProperty("Duplicated Property", 1000L, 45, BigDecimal.valueOf(26), 2020);

        mockMvc.perform(post("/api/ai/compare")
                        .header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "propertyIds": [%d, %d],
                                  "question": "비교해줘"
                                }
                                """.formatted(property.getPropertyId(), property.getPropertyId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }

    private User saveUser() {
        return userRepository.save(User.builder()
                .email("buyer@example.com")
                .password("encoded-password")
                .name("Buyer")
                .nickname("buyer")
                .role(Role.BUYER)
                .status(UserStatus.ACTIVE)
                .build());
    }

    private LifestyleResult saveLifestyleResult(Long userId) {
        return lifestyleResultRepository.save(LifestyleResult.builder()
                .userId(userId)
                .lifestyleType(LifestyleType.LIVING_COST_HOME_BALANCED)
                .livingConvenienceScore(2)
                .costSensitivityScore(2)
                .homeQualityScore(2)
                .facilityScoreMin(70)
                .facilityCountMin(20)
                .monthlyRentMax(50)
                .depositMax(1000L)
                .areaMin(BigDecimal.valueOf(25))
                .buildYearMin(2016)
                .build());
    }

    private Property saveProperty(String title, Long deposit, Integer monthlyRent, BigDecimal area, Integer buildYear) {
        return propertyRepository.save(Property.builder()
                .ownerId(null)
                .title(title)
                .address("Seoul Gangnam")
                .sido("Seoul")
                .gugun("Gangnam")
                .dong("Yeoksam")
                .rentType(RentType.MONTHLY)
                .roomType(RoomType.ONE_ROOM)
                .deposit(deposit)
                .monthlyRent(monthlyRent)
                .area(area)
                .buildYear(buildYear)
                .dataSource(DataSource.AGENT)
                .status(PropertyStatus.APPROVED)
                .build());
    }
}
