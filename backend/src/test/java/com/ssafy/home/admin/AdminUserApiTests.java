package com.ssafy.home.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssafy.home.auth.jwt.JwtTokenProvider;
import com.ssafy.home.auth.refresh.RefreshTokenRepository;
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
        "spring.datasource.url=jdbc:h2:mem:ssafy_home_admin_user;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class AdminUserApiTests {

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
    void adminCanReadUserListSearchAndDetail() throws Exception {
        User admin = saveUser("admin@example.com", "Admin", "admin", Role.ADMIN, null);
        User buyer = saveUser("buyer@example.com", "Buyer", "buyer", Role.BUYER, null);
        saveUser("agent@example.com", "Agent", "agent", Role.AGENT, "01012345678");
        String adminToken = tokenFor(admin);

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));

        mockMvc.perform(get("/api/admin/users")
                        .param("keyword", "buyer")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].email").value("buyer@example.com"));

        mockMvc.perform(get("/api/admin/users/{userId}", buyer.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(buyer.getId()))
                .andExpect(jsonPath("$.data.role").value("BUYER"));
    }

    @Test
    void adminCanUpdateUserStatusAndRole() throws Exception {
        User admin = saveUser("admin@example.com", "Admin", "admin", Role.ADMIN, null);
        User buyer = saveUser("buyer@example.com", "Buyer", "buyer", Role.BUYER, null);
        String adminToken = tokenFor(admin);

        mockMvc.perform(patch("/api/admin/users/{userId}/status", buyer.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "BANNED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("BANNED"));

        mockMvc.perform(patch("/api/admin/users/{userId}/role", buyer.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "AGENT",
                                  "phoneNumber": "01098765432"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("AGENT"))
                .andExpect(jsonPath("$.data.phoneNumber").value("01098765432"));

        User updated = userRepository.findById(buyer.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(UserStatus.BANNED);
        assertThat(updated.getRole()).isEqualTo(Role.AGENT);
        assertThat(updated.getPhoneNumber()).isEqualTo("01098765432");
    }

    @Test
    void nonAdminUsersCannotAccessAdminUserApis() throws Exception {
        User buyer = saveUser("buyer@example.com", "Buyer", "buyer", Role.BUYER, null);
        User agent = saveUser("agent@example.com", "Agent", "agent", Role.AGENT, "01012345678");
        String buyerToken = tokenFor(buyer);
        String agentToken = tokenFor(agent);

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/users/{userId}", buyer.getId())
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/admin/users/{userId}/status", buyer.getId())
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "BANNED"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/admin/users/{userId}/role", buyer.getId())
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousUserCannotAccessAdminUserApis() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCannotChangeUserToAgentWithoutPhoneNumber() throws Exception {
        User admin = saveUser("admin@example.com", "Admin", "admin", Role.ADMIN, null);
        User buyer = saveUser("buyer@example.com", "Buyer", "buyer", Role.BUYER, null);
        String adminToken = tokenFor(admin);

        mockMvc.perform(patch("/api/admin/users/{userId}/role", buyer.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "AGENT"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("PHONE_NUMBER_REQUIRED"));
    }

    @Test
    void adminCannotGrantAdminRole() throws Exception {
        User admin = saveUser("admin@example.com", "Admin", "admin", Role.ADMIN, null);
        User buyer = saveUser("buyer@example.com", "Buyer", "buyer", Role.BUYER, null);
        String adminToken = tokenFor(admin);

        mockMvc.perform(patch("/api/admin/users/{userId}/role", buyer.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_ROLE"));
    }

    @Test
    void adminCannotChangeOwnRole() throws Exception {
        User admin = saveUser("admin@example.com", "Admin", "admin", Role.ADMIN, null);
        String adminToken = tokenFor(admin);

        mockMvc.perform(patch("/api/admin/users/{userId}/role", admin.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "BUYER"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void adminCannotChangeOtherAdminRole() throws Exception {
        User admin = saveUser("admin@example.com", "Admin", "admin", Role.ADMIN, null);
        User otherAdmin = saveUser("other-admin@example.com", "Other Admin", "other-admin", Role.ADMIN, null);
        String adminToken = tokenFor(admin);

        mockMvc.perform(patch("/api/admin/users/{userId}/role", otherAdmin.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "BUYER"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_ROLE"));
    }

    private User saveUser(String email, String name, String nickname, Role role, String phoneNumber) {
        return userRepository.save(User.builder()
                .email(email)
                .password("encoded-password")
                .name(name)
                .nickname(nickname)
                .phoneNumber(phoneNumber)
                .role(role)
                .status(UserStatus.ACTIVE)
                .build());
    }

    private String tokenFor(User user) {
        return jwtTokenProvider.createAccessToken(user);
    }
}
