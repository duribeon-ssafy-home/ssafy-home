package com.ssafy.home.ai.service;

import com.ssafy.home.ai.client.RagServiceClient;
import com.ssafy.home.ai.dto.response.AiCompareSourceResponse;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class RagKnowledgeService {

    private static final String SYSTEM_PROMPT = """
            너는 한국 부동산 계약/전세사기 예방 상담사다.
            한국어로 답한다.
            제공된 문서 근거가 있을 때만 계약 체크리스트나 위험도 해설을 구체적으로 설명한다.
            문서 근거가 부족하면 모른다고 말하고, 추가 확인이 필요하다고 안내한다.
            """;

    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final RagServiceClient ragServiceClient;

    @Value("${app.ai.enabled:false}")
    private boolean aiEnabled;

    public RagAnswer answer(String message) {
        // Python RAG 서비스 우선 호출
        try {
            String pythonAnswer = ragServiceClient.chat(message);
            if (pythonAnswer != null && !pythonAnswer.isBlank()) {
                return new RagAnswer(pythonAnswer, List.of(), List.of());
            }
        } catch (RuntimeException ignored) {
            // Python RAG 서비스 미가동 시 Spring AI VectorStore로 폴백
        }

        // VectorStore 폴백
        RagContext context = retrieve(message, 5);
        if (!context.available()) {
            return new RagAnswer(
                    "아직 RAG 문서 저장소가 연결되지 않았습니다. 전세사기, 계약, 위험도 기준 문서를 추가하면 문서 근거 기반으로 답변할 수 있습니다.",
                    List.of(),
                    List.of("RAG VectorStore가 아직 설정되지 않았습니다.")
            );
        }

        if (context.documents().isEmpty()) {
            return new RagAnswer(
                    "관련 문서 근거를 찾지 못했습니다. 질문을 조금 더 구체적으로 입력해 주세요.",
                    List.of(),
                    List.of("질문과 직접 연결되는 RAG 문서를 찾지 못했습니다.")
            );
        }

        List<Document> documents = context.documents();
        List<AiCompareSourceResponse> sources = context.sources();
        if (!aiEnabled) {
            return new RagAnswer(
                    createDocumentSummaryAnswer(message, documents),
                    sources,
                    List.of()
            );
        }

        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            return new RagAnswer("AI 답변 생성 설정이 아직 준비되지 않았습니다.", sources, List.of());
        }

        try {
            String answer = builder.defaultSystem(SYSTEM_PROMPT)
                    .build()
                    .prompt()
                    .user(createPrompt(message, documents))
                    .call()
                    .content();
            return new RagAnswer(answer, sources, List.of());
        } catch (RuntimeException e) {
            return new RagAnswer("RAG 답변 생성 중 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.", sources, List.of());
        }
    }

    public RagContext retrieve(String message, int topK) {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            return new RagContext(false, List.of(), List.of());
        }

        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                .query(message)
                .topK(topK)
                .build());

        return new RagContext(true, documents, toSources(documents));
    }

    private String createPrompt(String message, List<Document> documents) {
        return """
                사용자 질문:
                %s

                검색된 문서 근거:
                %s
                """.formatted(message, documents.stream()
                .map(Document::getText)
                .toList());
    }

    private List<AiCompareSourceResponse> toSources(List<Document> documents) {
        List<AiCompareSourceResponse> sources = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            Document document = documents.get(i);
            String title = String.valueOf(document.getMetadata().getOrDefault("title", "RAG 문서"));
            String id = String.valueOf(document.getMetadata().getOrDefault("docId", "doc-" + (i + 1)));
            sources.add(new AiCompareSourceResponse("RAG", id, title));
        }
        return sources;
    }

    private String createDocumentSummaryAnswer(String message, List<Document> documents) {
        String titles = documents.stream()
                .map(document -> String.valueOf(document.getMetadata().getOrDefault("title", "RAG 문서")))
                .distinct()
                .limit(3)
                .toList()
                .toString();
        return """
                문서 근거를 검색했습니다.
                질문: %s
                참고한 문서: %s

                계약이나 전세사기 위험을 판단할 때는 등기부등본, 선순위 권리, 보증금과 주변 시세, 전입신고와 확정일자, 보증보험 가능 여부를 함께 확인해야 합니다.
                """.formatted(message, titles).strip();
    }

    public record RagAnswer(
            String answer,
            List<AiCompareSourceResponse> sources,
            List<String> warnings
    ) {
    }

    public record RagContext(
            boolean available,
            List<Document> documents,
            List<AiCompareSourceResponse> sources
    ) {
    }
}
