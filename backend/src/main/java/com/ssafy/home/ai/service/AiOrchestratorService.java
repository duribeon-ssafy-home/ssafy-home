package com.ssafy.home.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.home.ai.client.RagServiceClient;
import com.ssafy.home.ai.dto.request.AiChatRequest;
import com.ssafy.home.ai.dto.response.AiChatResponse;
import com.ssafy.home.ai.tool.LifestyleTool;
import com.ssafy.home.ai.tool.PropertyTool;
import com.ssafy.home.ai.tool.RagTool;
import com.ssafy.home.ai.tool.RiskTool;
import com.ssafy.home.lifestyle.service.LifestyleService;
import com.ssafy.home.property.service.PropertyService;
import com.ssafy.home.risk.service.RiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiOrchestratorService {

    private final ChatClient chatClient;
    private final PropertyService propertyService;
    private final RiskService riskService;
    private final LifestyleService lifestyleService;
    private final RagServiceClient ragServiceClient;
    private final ObjectMapper objectMapper;

    public AiChatResponse chat(AiChatRequest request, Long userId) {
        String userMessage = buildUserMessage(request.question(), request.propertyIds());

        String answer = chatClient.prompt()
                .user(userMessage)
                .tools(
                        new PropertyTool(propertyService, objectMapper),
                        new RiskTool(riskService, objectMapper),
                        new LifestyleTool(lifestyleService, objectMapper, userId),
                        new RagTool(ragServiceClient)
                )
                .call()
                .content();

        return new AiChatResponse(answer);
    }

    private String buildUserMessage(String question, List<Long> propertyIds) {
        if (propertyIds == null || propertyIds.isEmpty()) {
            return question;
        }
        return "비교 중인 매물 ID 목록: " + propertyIds + "\n\n사용자 질문: " + question;
    }
}
