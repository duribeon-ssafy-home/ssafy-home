package com.ssafy.home.recommendation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.ssafy.home.property.repository.AreaFacilityCountRepository;
import com.ssafy.home.property.repository.PropertyRepository;
import com.ssafy.home.user.entity.User;
import com.ssafy.home.user.repository.UserRepository;
import com.ssafy.home.user.type.Role;
import com.ssafy.home.user.type.UserStatus;
import java.math.BigDecimal;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:ssafy_home_recommendation;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class RecommendationApiTests {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @Autowired UserRepository userRepository;
    @Autowired PropertyRepository propertyRepository;
    @Autowired AreaFacilityCountRepository areaFacilityCountRepository;
    @Autowired LifestyleResultRepository lifestyleResultRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    private User buyer;
    private User agent;

    @BeforeEach
    void setUp() {
        areaFacilityCountRepository.deleteAll();
        propertyRepository.deleteAll();
        lifestyleResultRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        buyer = saveUser("buyer@test.com", Role.BUYER);
        agent = saveUser("agent@test.com", Role.AGENT);
    }

    @Test
    @DisplayName("추천 API는 사용자 검색 조건 안에서 설문 점수가 높은 매물을 먼저 반환한다")
    void recommendSortsByMatchScoreWithinUserFilters() throws Exception {
        saveLifestyleResult(buyer);

        saveProperty(b -> b.title("점수 높은 매물")
                .dong("역삼동")
                .roomType(RoomType.ONE_ROOM)
                .monthlyRent(45)
                .deposit(900L)
                .area(new BigDecimal("26.0"))
                .buildYear(2018)
                .status(PropertyStatus.APPROVED));
        saveArea("역삼동", 4, 4, 5, 4, 3, 3, 2);

        saveProperty(b -> b.title("조건 일부만 맞는 매물")
                .dong("서초동")
                .roomType(RoomType.ONE_ROOM)
                .monthlyRent(70)
                .deposit(900L)
                .area(new BigDecimal("20.0"))
                .buildYear(2010)
                .status(PropertyStatus.APPROVED));
        saveArea("서초동", 0, 0, 0, 0, 0, 0, 0);

        saveProperty(b -> b.title("사용자 월세 조건 제외 매물")
                .dong("삼성동")
                .roomType(RoomType.ONE_ROOM)
                .monthlyRent(90)
                .deposit(900L)
                .area(new BigDecimal("30.0"))
                .buildYear(2020)
                .status(PropertyStatus.APPROVED));
        saveArea("삼성동", 4, 4, 5, 4, 3, 3, 2);

        saveProperty(b -> b.title("사용자 방 타입 조건 제외 매물")
                .dong("논현동")
                .roomType(RoomType.OFFICETEL)
                .monthlyRent(45)
                .deposit(900L)
                .area(new BigDecimal("30.0"))
                .buildYear(2020)
                .status(PropertyStatus.APPROVED));
        saveArea("논현동", 4, 4, 5, 4, 3, 3, 2);

        mockMvc.perform(get("/api/recommendations")
                        .param("maxMonthlyRent", "80")
                        .param("roomType", "ONE_ROOM")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + tokenFor(buyer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].title").value("점수 높은 매물"))
                .andExpect(jsonPath("$.data.content[1].title").value("조건 일부만 맞는 매물"));
    }

    @Test
    @DisplayName("생활 성향 결과가 없으면 추천 API는 404를 반환한다")
    void recommendFailsWithoutLifestyleResult() throws Exception {
        mockMvc.perform(get("/api/recommendations")
                        .header("Authorization", "Bearer " + tokenFor(buyer)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("LIFESTYLE_RESULT_NOT_FOUND"));
    }

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

    private void saveLifestyleResult(User user) {
        lifestyleResultRepository.save(LifestyleResult.builder()
                .userId(user.getId())
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

    private Property saveProperty(UnaryOperator<Property.PropertyBuilder> configure) {
        Property.PropertyBuilder base = Property.builder()
                .ownerId(agent.getId())
                .title("테스트 매물")
                .address("서울특별시 강남구 역삼동 1")
                .sido("서울특별시")
                .gugun("강남구")
                .dong("역삼동")
                .rentType(RentType.MONTHLY)
                .roomType(RoomType.ONE_ROOM)
                .deposit(900L)
                .monthlyRent(45)
                .area(new BigDecimal("26.0"))
                .buildYear(2018)
                .status(PropertyStatus.APPROVED)
                .dataSource(DataSource.AGENT);
        return propertyRepository.save(configure.apply(base).build());
    }

    private void saveArea(
            String dong,
            int subwayCount500m,
            int martCount1km,
            int convenienceCount500m,
            int hospitalCount1km,
            int pharmacyCount500m,
            int cafeCount500m,
            int restaurantCount500m
    ) {
        jdbcTemplate.update("""
                INSERT INTO area_facility_counts (
                    sido,
                    gugun,
                    dong,
                    center_lat,
                    center_lng,
                    subway_count_500m,
                    mart_count_1km,
                    convenience_count_500m,
                    hospital_count_1km,
                    pharmacy_count_500m,
                    cafe_count_500m,
                    restaurant_count_500m,
                    calculated_at
                ) VALUES (
                    '서울특별시',
                    '강남구',
                    ?,
                    37.5000000,
                    127.0000000,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    CURRENT_TIMESTAMP
                )
                """,
                dong,
                subwayCount500m,
                martCount1km,
                convenienceCount500m,
                hospitalCount1km,
                pharmacyCount500m,
                cafeCount500m,
                restaurantCount500m
        );
    }

    private String tokenFor(User user) {
        return jwtTokenProvider.createAccessToken(user);
    }
}
