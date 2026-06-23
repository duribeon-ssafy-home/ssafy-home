package com.ssafy.home.ai.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Component
public class RagServiceClient {

    private final WebClient webClient;

    public RagServiceClient(@Value("${rag.service.url}") String ragServiceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(ragServiceUrl)
                .build();
    }

    public String chat(String question) {
        RagChatResult result = webClient.post()
                .uri("/api/rag/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("question", question))
                .retrieve()
                .bodyToMono(RagChatResult.class)
                .timeout(Duration.ofSeconds(30))
                .block();

        if (result == null || result.data() == null) {
            return "RAG 서비스에서 응답을 받지 못했습니다.";
        }
        return result.data().answer();
    }

    record RagChatResult(boolean success, String message, RagData data) {}
    record RagData(String answer, String query) {}
}
