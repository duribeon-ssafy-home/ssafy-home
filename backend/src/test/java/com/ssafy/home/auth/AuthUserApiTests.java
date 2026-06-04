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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
