package com.ssafy.home.lifestyle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssafy.home.auth.jwt.JwtTokenProvider;
import com.ssafy.home.auth.refresh.RefreshTokenRepository;
import com.ssafy.home.lifestyle.entity.LifestyleResult;
import com.ssafy.home.lifestyle.repository.LifestyleResultRepository;
import com.ssafy.home.lifestyle.type.LifestyleType;
import com.ssafy.home.user.entity.User;
import com.ssafy.home.user.repository.UserRepository;
import com.ssafy.home.user.type.Role;
import com.ssafy.home.user.type.UserStatus;
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
        "spring.datasource.url=jdbc:h2:mem:ssafy_home_lifestyle;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class LifestyleApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private LifestyleResultRepository lifestyleResultRepository;

    @BeforeEach
    void setUp() {
        lifestyleResultRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void publicUserCanReadLifestyleQuestions() throws Exception {
        mockMvc.perform(get("/api/lifestyle/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(6))
                .andExpect(jsonPath("$.data[0].questionId").value(1))
                .andExpect(jsonPath("$.data[0].category").value("LIVING_CONVENIENCE"))
                .andExpect(jsonPath("$.data[0].mapping").value("facilityScore"))
                .andExpect(jsonPath("$.data[1].mapping").value("facilityCount, facilityScore"));
    }

    @Test
    @DisplayName("비로그인 사용자는 생활 성향 결과를 저장 없이 미리보기할 수 있다")
    void publicUserCanPreviewLifestyleResultWithoutSaving() throws Exception {
        mockMvc.perform(post("/api/lifestyle/results/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answersJson("A", "B", "A", "B", "A", "B")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.lifestyleType").value("LIVING_COST_HOME_BALANCED"))
                .andExpect(jsonPath("$.data.typeName").value("균형 잡힌 생활권 실속형"))
                .andExpect(jsonPath("$.data.filterPreset.facilityScoreMin").value(70))
                .andExpect(jsonPath("$.data.filterPreset.monthlyRentMax").value(50))
                .andExpect(jsonPath("$.data.filterPreset.areaMin").value(25))
                .andExpect(jsonPath("$.data.filterPreset.facilityCountMin").doesNotExist())
                .andExpect(jsonPath("$.data.filterPreset.depositMax").doesNotExist())
                .andExpect(jsonPath("$.data.filterPreset.buildYearMin").doesNotExist());

        assertThat(lifestyleResultRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("비로그인 사용자는 생활 성향 결과를 저장할 수 없다")
    void publicUserCannotSaveLifestyleResult() throws Exception {
        mockMvc.perform(post("/api/lifestyle/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answersJson("A", "B", "A", "B", "A", "B")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void userCanSubmitLifestyleResultAndReceiveFilterPreset() throws Exception {
        User user = saveUser("buyer@example.com");
        String token = tokenFor(user);

        mockMvc.perform(post("/api/lifestyle/results")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answersJson("A", "A", "A", "A", "A", "A")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.lifestyleType").value("LIVING_COST_HOME_BALANCED"))
                .andExpect(jsonPath("$.data.typeName").value("균형 잡힌 생활권 실속형"))
                .andExpect(jsonPath("$.data.filterPreset.facilityScoreMin").value(70))
                .andExpect(jsonPath("$.data.filterPreset.facilityCountMin").value(20))
                .andExpect(jsonPath("$.data.filterPreset.monthlyRentMax").value(50))
                .andExpect(jsonPath("$.data.filterPreset.depositMax").value(1000))
                .andExpect(jsonPath("$.data.filterPreset.areaMin").value(25))
                .andExpect(jsonPath("$.data.filterPreset.buildYearMin").value(2016));

        LifestyleResult result = lifestyleResultRepository.findAll().get(0);
        assertThat(result.getUserId()).isEqualTo(user.getId());
        assertThat(result.getLifestyleType()).isEqualTo(LifestyleType.LIVING_COST_HOME_BALANCED);
        assertThat(result.getLivingConvenienceScore()).isEqualTo(2);
        assertThat(result.getCostSensitivityScore()).isEqualTo(2);
        assertThat(result.getHomeQualityScore()).isEqualTo(2);
    }

    @Test
    void userCanReadLatestLifestyleResult() throws Exception {
        User user = saveUser("buyer@example.com");
        String token = tokenFor(user);

        mockMvc.perform(post("/api/lifestyle/results")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answersJson("B", "B", "B", "B", "B", "B")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/lifestyle/results")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answersJson("A", "A", "A", "A", "A", "A")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/lifestyle/results/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lifestyleType").value("LIVING_COST_HOME_BALANCED"))
                .andExpect(jsonPath("$.data.filterPreset.facilityScoreMin").value(70));
    }

    @Test
    void invalidLifestyleAnswersAreRejected() throws Exception {
        User user = saveUser("buyer@example.com");
        String token = tokenFor(user);

        mockMvc.perform(post("/api/lifestyle/results")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers": [
                                    { "questionId": 1, "selectedOption": "A" },
                                    { "questionId": 1, "selectedOption": "B" },
                                    { "questionId": 2, "selectedOption": "A" },
                                    { "questionId": 3, "selectedOption": "A" },
                                    { "questionId": 4, "selectedOption": "A" },
                                    { "questionId": 5, "selectedOption": "A" }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }

    @Test
    void userCannotReadMissingLifestyleResult() throws Exception {
        User user = saveUser("buyer@example.com");
        String token = tokenFor(user);

        mockMvc.perform(get("/api/lifestyle/results/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("LIFESTYLE_RESULT_NOT_FOUND"));
    }

    private User saveUser(String email) {
        return userRepository.save(User.builder()
                .email(email)
                .password("encoded-password")
                .name("Buyer")
                .nickname("buyer")
                .role(Role.BUYER)
                .status(UserStatus.ACTIVE)
                .build());
    }

    private String tokenFor(User user) {
        return jwtTokenProvider.createAccessToken(user);
    }

    private String answersJson(
            String answer1,
            String answer2,
            String answer3,
            String answer4,
            String answer5,
            String answer6
    ) {
        return """
                {
                  "answers": [
                    { "questionId": 1, "selectedOption": "%s" },
                    { "questionId": 2, "selectedOption": "%s" },
                    { "questionId": 3, "selectedOption": "%s" },
                    { "questionId": 4, "selectedOption": "%s" },
                    { "questionId": 5, "selectedOption": "%s" },
                    { "questionId": 6, "selectedOption": "%s" }
                  ]
                }
                """.formatted(answer1, answer2, answer3, answer4, answer5, answer6);
    }
}
