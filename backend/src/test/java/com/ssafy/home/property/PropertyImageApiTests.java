package com.ssafy.home.property;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssafy.home.auth.jwt.JwtTokenProvider;
import com.ssafy.home.auth.refresh.RefreshTokenRepository;
import com.ssafy.home.property.entity.DataSource;
import com.ssafy.home.property.entity.Property;
import com.ssafy.home.property.entity.PropertyStatus;
import com.ssafy.home.property.repository.PropertyImageRepository;
import com.ssafy.home.property.repository.PropertyRepository;
import com.ssafy.home.user.entity.User;
import com.ssafy.home.user.repository.UserRepository;
import com.ssafy.home.user.type.Role;
import com.ssafy.home.user.type.UserStatus;
import java.io.IOException;
import java.nio.file.Paths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.FileSystemUtils;

@SpringBootTest(properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:ssafy_home_image;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "file.upload-dir=./test-uploads"
})
@AutoConfigureMockMvc
class PropertyImageApiTests {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @Autowired UserRepository userRepository;
    @Autowired PropertyRepository propertyRepository;
    @Autowired PropertyImageRepository propertyImageRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;

    private User agent;
    private User buyer;
    private Property agentProperty;
    private Property publicProperty;

    @BeforeEach
    void setUp() {
        propertyImageRepository.deleteAll();
        propertyRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        agent = saveUser("agent@test.com", Role.AGENT);
        buyer = saveUser("buyer@test.com", Role.BUYER);
        agentProperty = saveProperty(agent, DataSource.AGENT);
        publicProperty = saveProperty(null, DataSource.PUBLIC);
    }

    @AfterEach
    void cleanUp() throws IOException {
        FileSystemUtils.deleteRecursively(Paths.get("./test-uploads"));
    }

    // ───────────────────────────────────────────
    // 인증/권한 (401, 403)
    // ───────────────────────────────────────────

    @Test
    void 토큰없이_이미지업로드시_401() throws Exception {
        mockMvc.perform(multipart("/api/properties/" + agentProperty.getPropertyId() + "/images")
                        .file(imageFile()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void BUYER가_이미지업로드시_403() throws Exception {
        mockMvc.perform(multipart("/api/properties/" + agentProperty.getPropertyId() + "/images")
                        .file(imageFile())
                        .header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(buyer)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void 다른_AGENT_매물에_이미지업로드시_403() throws Exception {
        User otherAgent = saveUser("other@test.com", Role.AGENT);

        mockMvc.perform(multipart("/api/properties/" + agentProperty.getPropertyId() + "/images")
                        .file(imageFile())
                        .header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(otherAgent)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("PROPERTY_ACCESS_DENIED"));
    }

    @Test
    void 토큰없이_이미지삭제시_401() throws Exception {
        mockMvc.perform(delete("/api/properties/" + agentProperty.getPropertyId() + "/images/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    // ───────────────────────────────────────────
    // 업로드 성공
    // ───────────────────────────────────────────

    @Test
    void AGENT가_자신의_매물에_이미지업로드_성공() throws Exception {
        mockMvc.perform(multipart("/api/properties/" + agentProperty.getPropertyId() + "/images")
                        .file(imageFile())
                        .header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(agent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].imageUrl").exists())
                .andExpect(jsonPath("$.data[0].sortOrder").value(0));
    }

    // ───────────────────────────────────────────
    // 업로드 제한
    // ───────────────────────────────────────────

    @Test
    void 공공데이터_매물에_이미지업로드시_403() throws Exception {
        mockMvc.perform(multipart("/api/properties/" + publicProperty.getPropertyId() + "/images")
                        .file(imageFile())
                        .header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(agent)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("PUBLIC_PROPERTY_IMAGE_NOT_ALLOWED"));
    }

    @Test
    void 잘못된_파일형식_업로드시_400() throws Exception {
        MockMultipartFile textFile = new MockMultipartFile(
                "files", "test.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart("/api/properties/" + agentProperty.getPropertyId() + "/images")
                        .file(textFile)
                        .header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(agent)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_IMAGE_FORMAT"));
    }

    @Test
    void 이미지_10장_초과시_400() throws Exception {
        String token = jwtTokenProvider.createAccessToken(agent);
        String url = "/api/properties/" + agentProperty.getPropertyId() + "/images";

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(multipart(url).file(imageFile())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(multipart(url).file(imageFile())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("IMAGE_LIMIT_EXCEEDED"));
    }

    // ───────────────────────────────────────────
    // 삭제
    // ───────────────────────────────────────────

    @Test
    void 이미지_삭제_성공() throws Exception {
        String token = jwtTokenProvider.createAccessToken(agent);

        mockMvc.perform(multipart("/api/properties/" + agentProperty.getPropertyId() + "/images")
                        .file(imageFile())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        Long imageId = propertyImageRepository
                .findByProperty_PropertyId(agentProperty.getPropertyId())
                .get(0).getImageId();

        mockMvc.perform(delete("/api/properties/" + agentProperty.getPropertyId() + "/images/" + imageId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void 이미지_순서_변경_성공() throws Exception {
        String token = jwtTokenProvider.createAccessToken(agent);

        mockMvc.perform(multipart("/api/properties/" + agentProperty.getPropertyId() + "/images")
                        .file(imageFile())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        Long imageId = propertyImageRepository
                .findByProperty_PropertyId(agentProperty.getPropertyId())
                .get(0).getImageId();

        mockMvc.perform(patch("/api/properties/" + agentProperty.getPropertyId() + "/images/" + imageId)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"sortOrder\": 2}")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sortOrder").value(2));
    }

    @Test
    void 없는_이미지_삭제시_404() throws Exception {
        mockMvc.perform(delete("/api/properties/" + agentProperty.getPropertyId() + "/images/99999")
                        .header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(agent)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("IMAGE_NOT_FOUND"));
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

    private Property saveProperty(User owner, DataSource dataSource) {
        return propertyRepository.save(Property.builder()
                .ownerId(owner != null ? owner.getId() : null)
                .title("테스트 매물")
                .address("서울특별시 강남구 역삼동 1")
                .sido("서울특별시")
                .gugun("강남구")
                .dong("역삼동")
                .dataSource(dataSource)
                .status(PropertyStatus.APPROVED)
                .build());
    }

    private MockMultipartFile imageFile() {
        return new MockMultipartFile(
                "files", "test.jpg", "image/jpeg", "fake-image-content".getBytes());
    }
}
