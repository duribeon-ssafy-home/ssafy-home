package com.ssafy.home.property;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.ssafy.home.user.entity.User;
import com.ssafy.home.user.repository.UserRepository;
import com.ssafy.home.user.type.Role;
import com.ssafy.home.user.type.UserStatus;
import java.math.BigDecimal;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:ssafy_home_property_search;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class PropertySearchApiTests {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @Autowired UserRepository userRepository;
    @Autowired PropertyRepository propertyRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;

    private User agent;

    @BeforeEach
    void setUp() {
        propertyRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        agent = saveUser("agent@test.com", Role.AGENT);
    }

    // ───────────────────────────────────────────
    // 인증 (401 케이스)
    // ───────────────────────────────────────────

    @Test
    void 토큰없이_매물목록_조회는_성공한다() throws Exception {
        saveProperty(b -> b.status(PropertyStatus.APPROVED));

        mockMvc.perform(get("/api/properties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void 빈_문자열_검색조건은_전체조회처럼_동작한다() throws Exception {
        saveProperty(b -> b.status(PropertyStatus.APPROVED));
        saveProperty(b -> b.status(PropertyStatus.APPROVED));

        mockMvc.perform(get("/api/properties?sido=&gugun=&dong="))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    void 토큰없이_매물등록시_401() throws Exception {
        mockMvc.perform(post("/api/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(minimalCreateJson()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void 토큰없이_내매물조회시_401() throws Exception {
        mockMvc.perform(get("/api/properties/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    // ───────────────────────────────────────────
    // 페이지네이션
    // ───────────────────────────────────────────

    @Test
    void 매물5개에서_페이지사이즈2_첫페이지는_2건() throws Exception {
        for (int i = 0; i < 5; i++) {
            saveProperty(b -> b.status(PropertyStatus.APPROVED));
        }

        mockMvc.perform(get("/api/properties?page=0&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(5))
                .andExpect(jsonPath("$.data.totalPages").value(3));
    }

    @Test
    void 매물5개에서_마지막페이지는_1건() throws Exception {
        for (int i = 0; i < 5; i++) {
            saveProperty(b -> b.status(PropertyStatus.APPROVED));
        }

        mockMvc.perform(get("/api/properties?page=2&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    // ───────────────────────────────────────────
    // DELETED 제외
    // ───────────────────────────────────────────

    @Test
    void 삭제된매물은_목록에_포함되지_않는다() throws Exception {
        saveProperty(b -> b.status(PropertyStatus.APPROVED));
        saveProperty(b -> b.status(PropertyStatus.DELETED));

        mockMvc.perform(get("/api/properties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    // ───────────────────────────────────────────
    // 지역 필터
    // ───────────────────────────────────────────

    @Test
    void sido_필터로_서울매물만_조회된다() throws Exception {
        saveProperty(b -> b.sido("서울특별시").gugun("강남구").dong("역삼동").status(PropertyStatus.APPROVED));
        saveProperty(b -> b.sido("부산광역시").gugun("해운대구").dong("우동").status(PropertyStatus.APPROVED));

        mockMvc.perform(get("/api/properties?sido=서울특별시"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].sido").value("서울특별시"));
    }

    @Test
    void gugun_필터로_강남구만_조회된다() throws Exception {
        saveProperty(b -> b.sido("서울특별시").gugun("강남구").dong("역삼동").status(PropertyStatus.APPROVED));
        saveProperty(b -> b.sido("서울특별시").gugun("마포구").dong("합정동").status(PropertyStatus.APPROVED));

        mockMvc.perform(get("/api/properties?sido=서울특별시&gugun=강남구"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].gugun").value("강남구"));
    }

    // ───────────────────────────────────────────
    // 매물 유형 필터
    // ───────────────────────────────────────────

    @Test
    void rentType_MONTHLY_필터() throws Exception {
        saveProperty(b -> b.rentType(RentType.MONTHLY).monthlyRent(70).status(PropertyStatus.APPROVED));
        saveProperty(b -> b.rentType(RentType.JEONSE).deposit(30_000L).status(PropertyStatus.APPROVED));

        mockMvc.perform(get("/api/properties?rentType=MONTHLY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].rentType").value("MONTHLY"));
    }

    @Test
    void roomType_ONE_ROOM_필터() throws Exception {
        saveProperty(b -> b.roomType(RoomType.ONE_ROOM).status(PropertyStatus.APPROVED));
        saveProperty(b -> b.roomType(RoomType.APARTMENT).status(PropertyStatus.APPROVED));

        mockMvc.perform(get("/api/properties?roomType=ONE_ROOM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].roomType").value("ONE_ROOM"));
    }

    // ───────────────────────────────────────────
    // 가격 범위 필터
    // ───────────────────────────────────────────

    @Test
    void 보증금_범위_필터() throws Exception {
        saveProperty(b -> b.deposit(5_000L).status(PropertyStatus.APPROVED));
        saveProperty(b -> b.deposit(20_000L).status(PropertyStatus.APPROVED));
        saveProperty(b -> b.deposit(40_000L).status(PropertyStatus.APPROVED));

        mockMvc.perform(get("/api/properties?minDeposit=10000&maxDeposit=30000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].deposit").value(20_000));
    }

    @Test
    void 월세_최대값_필터() throws Exception {
        saveProperty(b -> b.rentType(RentType.MONTHLY).monthlyRent(50).status(PropertyStatus.APPROVED));
        saveProperty(b -> b.rentType(RentType.MONTHLY).monthlyRent(90).status(PropertyStatus.APPROVED));

        mockMvc.perform(get("/api/properties?maxMonthlyRent=70"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].monthlyRent").value(50));
    }

    // ───────────────────────────────────────────
    // 면적 범위 필터
    // ───────────────────────────────────────────

    @Test
    void 면적_범위_필터() throws Exception {
        saveProperty(b -> b.area(new BigDecimal("20.0")).status(PropertyStatus.APPROVED));
        saveProperty(b -> b.area(new BigDecimal("60.0")).status(PropertyStatus.APPROVED));
        saveProperty(b -> b.area(new BigDecimal("100.0")).status(PropertyStatus.APPROVED));

        mockMvc.perform(get("/api/properties?minArea=30&maxArea=80"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].area").value(60.0));
    }

    // ───────────────────────────────────────────
    // 복합 필터
    // ───────────────────────────────────────────

    @Test
    void 복합필터_sido_rentType_maxMonthlyRent() throws Exception {
        saveProperty(b -> b.sido("서울특별시").gugun("강남구").dong("역삼동")
                .rentType(RentType.MONTHLY).monthlyRent(60).status(PropertyStatus.APPROVED));
        saveProperty(b -> b.sido("서울특별시").gugun("강남구").dong("역삼동")
                .rentType(RentType.MONTHLY).monthlyRent(100).status(PropertyStatus.APPROVED));
        saveProperty(b -> b.sido("부산광역시").gugun("해운대구").dong("우동")
                .rentType(RentType.MONTHLY).monthlyRent(60).status(PropertyStatus.APPROVED));

        mockMvc.perform(get("/api/properties?sido=서울특별시&rentType=MONTHLY&maxMonthlyRent=70"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].sido").value("서울특별시"))
                .andExpect(jsonPath("$.data.content[0].monthlyRent").value(60));
    }

    // ───────────────────────────────────────────
    // 정렬
    // ───────────────────────────────────────────

    @Test
    void 보증금_오름차순_정렬() throws Exception {
        saveProperty(b -> b.deposit(30_000L).status(PropertyStatus.APPROVED));
        saveProperty(b -> b.deposit(10_000L).status(PropertyStatus.APPROVED));
        saveProperty(b -> b.deposit(20_000L).status(PropertyStatus.APPROVED));

        mockMvc.perform(get("/api/properties?sort=deposit,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].deposit").value(10_000))
                .andExpect(jsonPath("$.data.content[1].deposit").value(20_000))
                .andExpect(jsonPath("$.data.content[2].deposit").value(30_000));
    }

    @Test
    void 월세_내림차순_정렬() throws Exception {
        saveProperty(b -> b.rentType(RentType.MONTHLY).monthlyRent(50).status(PropertyStatus.APPROVED));
        saveProperty(b -> b.rentType(RentType.MONTHLY).monthlyRent(90).status(PropertyStatus.APPROVED));
        saveProperty(b -> b.rentType(RentType.MONTHLY).monthlyRent(70).status(PropertyStatus.APPROVED));

        mockMvc.perform(get("/api/properties?sort=monthlyRent,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].monthlyRent").value(90))
                .andExpect(jsonPath("$.data.content[1].monthlyRent").value(70))
                .andExpect(jsonPath("$.data.content[2].monthlyRent").value(50));
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

    private Property saveProperty(UnaryOperator<Property.PropertyBuilder> configure) {
        Property.PropertyBuilder base = Property.builder()
                .ownerId(agent.getId())
                .title("테스트 매물")
                .address("서울특별시 강남구 역삼동 1")
                .sido("서울특별시")
                .gugun("강남구")
                .dong("역삼동")
                .dataSource(DataSource.AGENT);
        return propertyRepository.save(configure.apply(base).build());
    }

    private String minimalCreateJson() {
        return """
                {
                  "title": "등록 테스트",
                  "address": "서울특별시 강남구 역삼동 1",
                  "sido": "서울특별시",
                  "gugun": "강남구",
                  "dong": "역삼동"
                }
                """;
    }
}
