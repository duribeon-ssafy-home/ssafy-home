package com.ssafy.home.location;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssafy.home.auth.refresh.RefreshTokenRepository;
import com.ssafy.home.location.entity.LegalDong;
import com.ssafy.home.location.repository.LegalDongRepository;
import com.ssafy.home.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:ssafy_home_location;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
class LocationApiTests {

    @Autowired MockMvc mockMvc;
    @Autowired EntityManager entityManager;
    @Autowired LegalDongRepository legalDongRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired UserRepository userRepository;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        legalDongRepository.deleteAll();
    }

    @Test
    @DisplayName("지역 자동완성은 로그인 없이 조회할 수 있다")
    void searchLocationsWithoutToken() throws Exception {
        saveLegalDong("2638010300", "부산광역시", "사하구", "하단동", "부산광역시 사하구 하단동", true);

        mockMvc.perform(get("/api/locations?keyword=하단"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("2638010300"))
                .andExpect(jsonPath("$.data[0].sido").value("부산광역시"))
                .andExpect(jsonPath("$.data[0].gugun").value("사하구"))
                .andExpect(jsonPath("$.data[0].dong").value("하단동"))
                .andExpect(jsonPath("$.data[0].fullName").value("부산광역시 사하구 하단동"));
    }

    @Test
    @DisplayName("두 글자 미만 검색어는 빈 후보 목록을 반환한다")
    void shortKeywordReturnsEmptyList() throws Exception {
        saveLegalDong("2638010300", "부산광역시", "사하구", "하단동", "부산광역시 사하구 하단동", true);

        mockMvc.perform(get("/api/locations?keyword=하"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("폐지된 법정동은 자동완성 후보에서 제외한다")
    void inactiveLegalDongIsExcluded() throws Exception {
        saveLegalDong("2638010300", "부산광역시", "사하구", "하단동", "부산광역시 사하구 하단동", false);

        mockMvc.perform(get("/api/locations?keyword=하단"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    private void saveLegalDong(
            String code,
            String sido,
            String gugun,
            String dong,
            String fullName,
            boolean active
    ) {
        entityManager.createNativeQuery("""
                        INSERT INTO legal_dongs (code, sido, gugun, dong, full_name, active)
                        VALUES (:code, :sido, :gugun, :dong, :fullName, :active)
                        """)
                .setParameter("code", code)
                .setParameter("sido", sido)
                .setParameter("gugun", gugun)
                .setParameter("dong", dong)
                .setParameter("fullName", fullName)
                .setParameter("active", active)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }
}
