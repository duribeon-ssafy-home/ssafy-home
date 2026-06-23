package com.ssafy.home.ai.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.home.ai.dto.response.AiCompareSourceResponse;
import com.ssafy.home.ai.dto.response.AiPropertyAnalysisResponse;
import com.ssafy.home.lifestyle.dto.response.LifestyleResultResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class FinalAnswerAgent {

    private static final String SYSTEM_PROMPT = """
            You are a Korean real-estate comparison assistant.
            Answer in Korean.
            Use only the provided comparison data.
            Use provided RAG document excerpts only for contract, fraud-prevention, risk-score, market-price, or terminology explanations.
            Do not invent property details, legal rules, neighborhood facts, or policy information.
            Explain risk labels and scores without exaggeration.
            If the retrieved document excerpts do not support a claim, say that additional verification is needed.
            Keep the answer concise and practical for a user comparing 2 to 4 listings.
            Do not use markdown formatting (**, ##, bullet points, etc.). Answer in plain text only.
            """;

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.enabled:false}")
    private boolean enabled;

    public String generate(
            String question,
            LifestyleResultResponse lifestyle,
            List<AiPropertyAnalysisResponse> analyses,
            List<AiCompareSourceResponse> sources,
            List<Document> ragDocuments,
            String fallbackAnswer
    ) {
        if (!enabled) {
            return fallbackAnswer;
        }

        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            return fallbackAnswer;
        }

        try {
            return builder
                    .defaultSystem(SYSTEM_PROMPT)
                    .build()
                    .prompt()
                    .user(createUserPrompt(question, lifestyle, analyses, sources, ragDocuments))
                    .call()
                    .content();
        } catch (RuntimeException | JsonProcessingException e) {
            return fallbackAnswer;
        }
    }

    private String createUserPrompt(
            String question,
            LifestyleResultResponse lifestyle,
            List<AiPropertyAnalysisResponse> analyses,
            List<AiCompareSourceResponse> sources,
            List<Document> ragDocuments
    ) throws JsonProcessingException {
        AiFinalAnswerContext context = new AiFinalAnswerContext(
                question,
                lifestyle,
                analyses,
                sources,
                ragDocuments.stream()
                        .map(document -> new RagExcerpt(
                                String.valueOf(document.getMetadata().getOrDefault("title", "RAG 문서")),
                                document.getText()
                        ))
                        .toList()
        );
        return """
                User question:
                %s

                Grounded comparison data and document excerpts:
                %s

                Write:
                1. One recommended listing and the reason.
                2. Two to four key comparison points.
                3. One document-grounded contract/risk caution when relevant.
                """.formatted(question, objectMapper.writeValueAsString(context));
    }

    private record AiFinalAnswerContext(
            String question,
            LifestyleResultResponse lifestyle,
            List<AiPropertyAnalysisResponse> analyses,
            List<AiCompareSourceResponse> sources,
            List<RagExcerpt> ragExcerpts
    ) {
    }

    private record RagExcerpt(
            String title,
            String text
    ) {
    }
}
