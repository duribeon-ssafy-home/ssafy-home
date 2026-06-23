package com.ssafy.home.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.home.ai.dto.request.AiChatRequest;
import com.ssafy.home.ai.dto.response.AiChatResponse;
import com.ssafy.home.ai.service.AiOrchestratorService;
import com.ssafy.home.auth.jwt.JwtAuthentication;
import com.ssafy.home.auth.jwt.JwtTokenProvider;
import com.ssafy.home.user.type.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:ssafy_home_ai;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.ai.openai.api-key=test-key",
        "rag.service.url=http://localhost:8000"
})
@AutoConfigureMockMvc
class AiControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean AiOrchestratorService aiOrchestratorService;

    private static final JwtAuthentication AUTH = new JwtAuthentication(1L, "test@test.com", Role.BUYER);
    private static final UsernamePasswordAuthenticationToken TOKEN =
            new UsernamePasswordAuthenticationToken(AUTH, null, List.of());

    @Test
    void chat_returnsAnswer() throws Exception {
        when(aiOrchestratorService.chat(any(), eq(1L)))
                .thenReturn(new AiChatResponse("전세사기를 예방하려면 등기부등본을 확인하세요."));

        AiChatRequest request = new AiChatRequest("전세사기 어떻게 피해?", List.of());

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(TOKEN))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.answer").value("전세사기를 예방하려면 등기부등본을 확인하세요."));
    }

    @Test
    void chat_withoutAuth_returns401() throws Exception {
        AiChatRequest request = new AiChatRequest("질문", List.of());

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
