package com.ssafy.home.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.home.auth.refresh.RefreshTokenRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.url=jdbc:h2:mem:ssafy_home_auth;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class AuthUserApiTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void setUp() {
		refreshTokenRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void buyerCanSignupWithoutPhoneNumber() throws Exception {
		mockMvc.perform(post("/api/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "buyer@example.com",
								  "password": "password123!",
								  "name": "Buyer",
								  "nickname": "buyer",
								  "role": "BUYER"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.email").value("buyer@example.com"))
				.andExpect(jsonPath("$.data.role").value("BUYER"))
				.andExpect(jsonPath("$.data.status").value("ACTIVE"));

		assertThat(userRepository.findByEmail("buyer@example.com")).isPresent();
	}

	@Test
	void agentSignupRequiresPhoneNumber() throws Exception {
		mockMvc.perform(post("/api/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "agent@example.com",
								  "password": "password123!",
								  "name": "Agent",
								  "nickname": "agent",
								  "role": "AGENT"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.errorCode").value("PHONE_NUMBER_REQUIRED"));
	}

	@Test
	@DisplayName("회원가입 비밀번호는 8자 이상이어야 한다")
	void signupPasswordRequiresAtLeastEightCharacters() throws Exception {
		mockMvc.perform(post("/api/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "short-password@example.com",
								  "password": "short",
								  "name": "Buyer",
								  "nickname": "buyer",
								  "role": "BUYER"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false));
	}

	@Test
	void adminCannotSignup() throws Exception {
		mockMvc.perform(post("/api/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "admin@example.com",
								  "password": "password123!",
								  "name": "Admin",
								  "nickname": "admin",
								  "role": "ADMIN"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.errorCode").value("INVALID_ROLE"));
	}

	@Test
	void loginRefreshAndMeWorkWithJwt() throws Exception {
		signupBuyer("buyer@example.com");

		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "buyer@example.com",
								  "password": "password123!"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accessToken").isString())
				.andExpect(jsonPath("$.data.refreshToken").isString())
				.andExpect(jsonPath("$.data.user.email").value("buyer@example.com"))
				.andReturn();

		JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
		String accessToken = loginJson.at("/data/accessToken").asText();
		String refreshToken = loginJson.at("/data/refreshToken").asText();

		mockMvc.perform(get("/api/auth/me")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.email").value("buyer@example.com"))
				.andExpect(jsonPath("$.data.role").value("BUYER"));

		mockMvc.perform(post("/api/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "refreshToken": "%s"
								}
								""".formatted(refreshToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accessToken").isString())
				.andExpect(jsonPath("$.data.refreshToken").isString());
	}

	@Test
	void logoutRevokesRefreshTokenWithoutAccessToken() throws Exception {
		signupBuyer("buyer@example.com");

		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "buyer@example.com",
								  "password": "password123!"
								}
								"""))
				.andExpect(status().isOk())
				.andReturn();

		JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
		String refreshToken = loginJson.at("/data/refreshToken").asText();

		mockMvc.perform(post("/api/auth/logout")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "refreshToken": "%s"
								}
								""".formatted(refreshToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

		assertThat(refreshTokenRepository.findByToken(refreshToken).orElseThrow().isValid()).isFalse();
	}

	@Test
	@DisplayName("임시 비밀번호 발급은 이메일 존재 여부와 관계없이 성공 응답을 반환한다")
	void forgotPasswordDoesNotExposeEmailExistence() throws Exception {
		mockMvc.perform(post("/api/auth/password/forgot")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "missing@example.com"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));
	}

	@Test
	@DisplayName("임시 비밀번호 발급 시 기존 비밀번호와 refresh token을 무효화한다")
	void forgotPasswordReplacesPasswordAndDeletesRefreshTokens() throws Exception {
		signupAndLogin("buyer@example.com");
		assertThat(refreshTokenRepository.count()).isEqualTo(1);

		mockMvc.perform(post("/api/auth/password/forgot")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "buyer@example.com"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

		var user = userRepository.findByEmail("buyer@example.com").orElseThrow();
		assertThat(passwordEncoder.matches("password123!", user.getPassword())).isFalse();
		assertThat(refreshTokenRepository.count()).isZero();
	}

	@Test
	void inactiveUserCannotUseIssuedTokens() throws Exception {
		signupBuyer("inactive@example.com");

		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "inactive@example.com",
								  "password": "password123!"
								}
								"""))
				.andExpect(status().isOk())
				.andReturn();

		JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
		String accessToken = loginJson.at("/data/accessToken").asText();
		String refreshToken = loginJson.at("/data/refreshToken").asText();

		var user = userRepository.findByEmail("inactive@example.com").orElseThrow();
		user.changeStatus(UserStatus.INACTIVE);
		userRepository.saveAndFlush(user);

		mockMvc.perform(get("/api/auth/me")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

		mockMvc.perform(post("/api/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "refreshToken": "%s"
								}
								""".formatted(refreshToken)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value("INACTIVE_USER"));
	}

	@Test
	void userCanReadAndUpdateProfile() throws Exception {
		String accessToken = signupAndLogin("buyer@example.com");

		mockMvc.perform(get("/api/users/me")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.email").value("buyer@example.com"))
				.andExpect(jsonPath("$.data.name").value("Buyer"));

		mockMvc.perform(patch("/api/users/me")
						.header("Authorization", "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Updated Buyer",
								  "nickname": "updated",
								  "phoneNumber": "01012345678"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.name").value("Updated Buyer"))
				.andExpect(jsonPath("$.data.nickname").value("updated"))
				.andExpect(jsonPath("$.data.phoneNumber").value("01012345678"));
	}

	@Test
	@DisplayName("사용자는 마이페이지에서 비밀번호를 변경할 수 있다")
	void userCanChangePassword() throws Exception {
		String accessToken = signupAndLogin("buyer@example.com");

		mockMvc.perform(patch("/api/users/me/password")
						.header("Authorization", "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "currentPassword": "password123!",
								  "newPassword": "newPassword123!"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "buyer@example.com",
								  "password": "password123!"
								}
								"""))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "buyer@example.com",
								  "password": "newPassword123!"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.user.email").value("buyer@example.com"));
	}

	@Test
	void buyerCanChangeRoleToAgentWithPhoneNumber() throws Exception {
		String accessToken = signupAndLogin("buyer@example.com");

		mockMvc.perform(patch("/api/users/me/role")
						.header("Authorization", "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "role": "AGENT",
								  "phoneNumber": "01012345678"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.role").value("AGENT"))
				.andExpect(jsonPath("$.data.phoneNumber").value("01012345678"));

		assertThat(userRepository.findByEmail("buyer@example.com").orElseThrow().getRole()).isEqualTo(Role.AGENT);
	}

	@Test
	@DisplayName("BUYER는 전화번호 없이 AGENT로 전환할 수 없다")
	void buyerCannotChangeRoleToAgentWithoutPhoneNumber() throws Exception {
		String accessToken = signupAndLogin("buyer@example.com");

		mockMvc.perform(patch("/api/users/me/role")
						.header("Authorization", "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "role": "AGENT"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.errorCode").value("PHONE_NUMBER_REQUIRED"));

		assertThat(userRepository.findByEmail("buyer@example.com").orElseThrow().getRole()).isEqualTo(Role.BUYER);
	}

	@Test
	void userCanChangeStatusToInactive() throws Exception {
		String accessToken = signupAndLogin("buyer@example.com");

		mockMvc.perform(patch("/api/users/me/status")
						.header("Authorization", "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "status": "INACTIVE"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("INACTIVE"));

		assertThat(userRepository.findByEmail("buyer@example.com").orElseThrow().getStatus()).isEqualTo(UserStatus.INACTIVE);
	}

	@Test
	@DisplayName("사용자는 본인 계정을 탈퇴 상태로 변경할 수 있다")
	void userCanChangeStatusToDeleted() throws Exception {
		String accessToken = signupAndLogin("buyer@example.com");

		mockMvc.perform(patch("/api/users/me/status")
						.header("Authorization", "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "status": "DELETED"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.status").value("DELETED"));

		assertThat(userRepository.findByEmail("buyer@example.com").orElseThrow().getStatus()).isEqualTo(UserStatus.DELETED);
	}

	private void signupBuyer(String email) throws Exception {
		mockMvc.perform(post("/api/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "password123!",
								  "name": "Buyer",
								  "nickname": "buyer",
								  "role": "BUYER"
								}
								""".formatted(email)))
				.andExpect(status().isOk());
	}

	private String signupAndLogin(String email) throws Exception {
		signupBuyer(email);

		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "password123!"
								}
								""".formatted(email)))
				.andExpect(status().isOk())
				.andReturn();

		return objectMapper.readTree(loginResult.getResponse().getContentAsString())
				.at("/data/accessToken")
				.asText();
	}
}
