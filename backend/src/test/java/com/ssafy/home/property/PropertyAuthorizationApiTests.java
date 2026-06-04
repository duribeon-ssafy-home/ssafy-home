package com.ssafy.home.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssafy.home.auth.jwt.JwtTokenProvider;
import com.ssafy.home.auth.refresh.RefreshTokenRepository;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:ssafy_home_property_auth;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class PropertyAuthorizationApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        propertyRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void publicUserCanReadPropertyListAndDetail() throws Exception {
        Property property = saveProperty(null, PropertyStatus.APPROVED);

        mockMvc.perform(get("/api/properties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/properties/{propertyId}", property.getPropertyId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.propertyId").value(property.getPropertyId()));
    }

    @Test
    void buyerCannotCreateUpdateDeleteOrReadMyProperties() throws Exception {
        User buyer = saveUser("buyer@example.com", Role.BUYER);
        User agent = saveUser("agent@example.com", Role.AGENT);
        Property property = saveProperty(agent.getId(), PropertyStatus.APPROVED);
        String buyerToken = tokenFor(buyer);

        mockMvc.perform(post("/api/properties")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPropertyJson()))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/properties/{propertyId}", property.getPropertyId())
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePropertyJson("buyer blocked address")))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/properties/{propertyId}", property.getPropertyId())
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/properties/me")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void agentCanCreateAndReadOwnProperties() throws Exception {
        User agent = saveUser("agent@example.com", Role.AGENT);
        String agentToken = tokenFor(agent);

        mockMvc.perform(post("/api/properties")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPropertyJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/properties/me")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].ownerId").value(agent.getId()));
    }

    @Test
    void agentCanUpdateAndDeleteOwnPropertyOnly() throws Exception {
        User owner = saveUser("owner@example.com", Role.AGENT);
        User otherAgent = saveUser("other-agent@example.com", Role.AGENT);
        Property ownerProperty = saveProperty(owner.getId(), PropertyStatus.APPROVED);
        Property otherProperty = saveProperty(otherAgent.getId(), PropertyStatus.APPROVED);
        String ownerToken = tokenFor(owner);

        mockMvc.perform(patch("/api/properties/{propertyId}", ownerProperty.getPropertyId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePropertyJson("updated address")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.address").value("updated address"));

        mockMvc.perform(patch("/api/properties/{propertyId}", otherProperty.getPropertyId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePropertyJson("blocked address")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("PROPERTY_ACCESS_DENIED"));

        mockMvc.perform(delete("/api/properties/{propertyId}", ownerProperty.getPropertyId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        assertThat(propertyRepository.findById(ownerProperty.getPropertyId()).orElseThrow().getStatus())
                .isEqualTo(PropertyStatus.DELETED);
    }

    @Test
    void adminCanUpdateAndDeleteAnyProperty() throws Exception {
        User admin = saveUser("admin@example.com", Role.ADMIN);
        User agent = saveUser("agent@example.com", Role.AGENT);
        Property property = saveProperty(agent.getId(), PropertyStatus.APPROVED);
        String adminToken = tokenFor(admin);

        mockMvc.perform(patch("/api/properties/{propertyId}", property.getPropertyId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePropertyJson("admin updated address")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.address").value("admin updated address"));

        mockMvc.perform(delete("/api/properties/{propertyId}", property.getPropertyId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertThat(propertyRepository.findById(property.getPropertyId()).orElseThrow().getStatus())
                .isEqualTo(PropertyStatus.DELETED);
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

    private Property saveProperty(Long ownerId, PropertyStatus status) {
        return propertyRepository.save(Property.builder()
                .ownerId(ownerId)
                .title("테스트 매물")
                .address("서울시 강남구 역삼동 1")
                .sido("서울시")
                .gugun("강남구")
                .dong("역삼동")
                .dataSource(ownerId == null ? DataSource.PUBLIC : DataSource.AGENT)
                .status(status)
                .build());
    }

    private String tokenFor(User user) {
        return jwtTokenProvider.createAccessToken(user);
    }

    private String createPropertyJson() {
        return """
                {
                  "title": "등록 매물",
                  "address": "서울시 강남구 대치동 1",
                  "sido": "서울시",
                  "gugun": "강남구",
                  "dong": "대치동"
                }
                """;
    }

    private String updatePropertyJson(String address) {
        return """
                {
                  "title": "수정 매물",
                  "address": "%s",
                  "sido": "서울시",
                  "gugun": "강남구",
                  "dong": "역삼동"
                }
                """.formatted(address);
    }
}
