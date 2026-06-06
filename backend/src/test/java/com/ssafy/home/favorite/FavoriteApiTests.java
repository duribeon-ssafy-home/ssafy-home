package com.ssafy.home.favorite;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssafy.home.auth.jwt.JwtTokenProvider;
import com.ssafy.home.auth.refresh.RefreshTokenRepository;
import com.ssafy.home.favorite.repository.FavoriteRepository;
import com.ssafy.home.property.entity.DataSource;
import com.ssafy.home.property.entity.Property;
import com.ssafy.home.property.entity.PropertyStatus;
import com.ssafy.home.property.repository.PropertyRepository;
import com.ssafy.home.user.entity.User;
import com.ssafy.home.user.repository.UserRepository;
import com.ssafy.home.user.type.Role;
import com.ssafy.home.user.type.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:ssafy_home_favorite;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class FavoriteApiTests {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @Autowired UserRepository userRepository;
    @Autowired PropertyRepository propertyRepository;
    @Autowired FavoriteRepository favoriteRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;

    private User buyer;
    private Property property;

    @BeforeEach
    void setUp() {
        favoriteRepository.deleteAll();
        propertyRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        buyer = saveUser("buyer@test.com", Role.BUYER);
        property = saveProperty(PropertyStatus.APPROVED);
    }

    // ───────────────────────────────────────────
    // 인증
    // ───────────────────────────────────────────

    @Test
    void 토큰없이_관심매물_추가시_401() throws Exception {
        mockMvc.perform(post("/api/favorites/{propertyId}", property.getPropertyId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void 토큰없이_관심매물_목록조회시_401() throws Exception {
        mockMvc.perform(get("/api/favorites"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    // ───────────────────────────────────────────
    // 추가
    // ───────────────────────────────────────────

    @Test
    void 관심매물_추가_성공() throws Exception {
        mockMvc.perform(post("/api/favorites/{propertyId}", property.getPropertyId())
                        .header("Authorization", "Bearer " + tokenFor(buyer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void 이미_추가된_매물_재추가시_CONFLICT() throws Exception {
        String token = tokenFor(buyer);

        mockMvc.perform(post("/api/favorites/{propertyId}", property.getPropertyId())
                .header("Authorization", "Bearer " + token));

        mockMvc.perform(post("/api/favorites/{propertyId}", property.getPropertyId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("FAVORITE_ALREADY_EXISTS"));
    }

    @Test
    void 존재하지_않는_매물_추가시_404() throws Exception {
        mockMvc.perform(post("/api/favorites/{propertyId}", 999L)
                        .header("Authorization", "Bearer " + tokenFor(buyer)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PROPERTY_NOT_FOUND"));
    }

    // ───────────────────────────────────────────
    // 삭제
    // ───────────────────────────────────────────

    @Test
    void 관심매물_삭제_성공() throws Exception {
        String token = tokenFor(buyer);

        mockMvc.perform(post("/api/favorites/{propertyId}", property.getPropertyId())
                .header("Authorization", "Bearer " + token));

        mockMvc.perform(delete("/api/favorites/{propertyId}", property.getPropertyId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void 관심매물에_없는_매물_삭제시_404() throws Exception {
        mockMvc.perform(delete("/api/favorites/{propertyId}", property.getPropertyId())
                        .header("Authorization", "Bearer " + tokenFor(buyer)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("FAVORITE_NOT_FOUND"));
    }

    // ───────────────────────────────────────────
    // 목록 조회
    // ───────────────────────────────────────────

    @Test
    void 관심매물_목록_조회_성공() throws Exception {
        String token = tokenFor(buyer);
        Property property2 = saveProperty(PropertyStatus.APPROVED);

        mockMvc.perform(post("/api/favorites/{propertyId}", property.getPropertyId())
                .header("Authorization", "Bearer " + token));
        mockMvc.perform(post("/api/favorites/{propertyId}", property2.getPropertyId())
                .header("Authorization", "Bearer " + token));

        mockMvc.perform(get("/api/favorites")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void 다른_사용자_관심매물은_조회되지_않는다() throws Exception {
        User other = saveUser("other@test.com", Role.BUYER);

        mockMvc.perform(post("/api/favorites/{propertyId}", property.getPropertyId())
                .header("Authorization", "Bearer " + tokenFor(other)));

        mockMvc.perform(get("/api/favorites")
                        .header("Authorization", "Bearer " + tokenFor(buyer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // ───────────────────────────────────────────
    // 헬퍼
    // ───────────────────────────────────────────

    private User saveUser(String email, Role role) {
        return userRepository.save(User.builder()
                .email(email)
                .password("encoded-password")
                .name(role.name())
                .nickname(email.substring(0, email.indexOf("@")))
                .role(role)
                .status(UserStatus.ACTIVE)
                .build());
    }

    private Property saveProperty(PropertyStatus status) {
        return propertyRepository.save(Property.builder()
                .title("테스트 매물")
                .address("서울특별시 강남구 역삼동 1")
                .sido("서울특별시")
                .gugun("강남구")
                .dong("역삼동")
                .dataSource(DataSource.PUBLIC)
                .status(status)
                .build());
    }

    private String tokenFor(User user) {
        return jwtTokenProvider.createAccessToken(user);
    }
}
