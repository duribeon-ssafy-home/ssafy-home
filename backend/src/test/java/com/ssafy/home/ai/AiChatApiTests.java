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
        "spring.datasource.url=jdbc:h2:mem:ssafy_home_ai_chat;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class AiChatApiTests {

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
    @DisplayName("AI 채팅은 비교 의도를 기존 비교 분석 흐름으로 라우팅한다")
    void chatRoutesCompareIntent() throws Exception {
        User user = saveUser();
        saveLifestyleResult(user.getId());
        Property first = saveProperty("First Property", 1000L);
        Property second = saveProperty("Second Property", 700L);

        mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "이 매물 두 개 비교해서 추천해줘",
                                  "propertyIds": [%d, %d]
                                }
                                """.formatted(first.getPropertyId(), second.getPropertyId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.intent").value("PROPERTY_COMPARE"))
                .andExpect(jsonPath("$.data.compare.recommendedPropertyId").exists());
    }

    @Test
    @DisplayName("AI 채팅은 비교 매물이 있으면 위험도 설명 문장도 비교 분석으로 라우팅한다")
    void chatRoutesRiskQuestionWithComparePropertiesToCompareIntent() throws Exception {
        User user = saveUser();
        Property first = saveProperty("First Property", 1000L);
        Property second = saveProperty("Second Property", 700L);

        mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "보증금이 싼데 위험 점수가 높은 매물을 설명해줘",
                                  "propertyIds": [%d, %d]
                                }
                                """.formatted(first.getPropertyId(), second.getPropertyId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.intent").value("PROPERTY_COMPARE"))
                .andExpect(jsonPath("$.data.compare.propertyAnalyses.length()").value(2));
    }

    @Test
    @DisplayName("AI 채팅은 비교 매물이 있어도 순수 계약 용어 질문은 RAG 흐름으로 라우팅한다")
    void chatRoutesContractConceptQuestionWithComparePropertiesToRagIntent() throws Exception {
        User user = saveUser();
        Property first = saveProperty("First Property", 1000L);
        Property second = saveProperty("Second Property", 700L);

        mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "반전세가 뭐야?",
                                  "propertyIds": [%d, %d]
                                }
                                """.formatted(first.getPropertyId(), second.getPropertyId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.intent").value("CONTRACT_KNOWLEDGE"))
                .andExpect(jsonPath("$.data.compare").doesNotExist())
                .andExpect(jsonPath("$.data.sources[0].type").value("RAG"));
    }

    @Test
    @DisplayName("AI 채팅은 계약 지식 질문을 RAG 흐름으로 라우팅한다")
    void chatRoutesContractKnowledgeIntentWithRagSources() throws Exception {
        User user = saveUser();

        mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "전세사기 피하려면 계약 전에 뭘 확인해야 해?"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.intent").value("CONTRACT_KNOWLEDGE"))
                .andExpect(jsonPath("$.data.verified").value(true))
                .andExpect(jsonPath("$.data.sources[0].type").value("RAG"));
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

    private Property saveProperty(String title, Long deposit) {
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
                .monthlyRent(45)
                .area(BigDecimal.valueOf(26))
                .buildYear(2020)
                .dataSource(DataSource.AGENT)
                .status(PropertyStatus.APPROVED)
                .build());
    }
}
