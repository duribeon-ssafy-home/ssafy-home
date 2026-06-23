package com.ssafy.home.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class GeneralLlmService {

    private static final String SYSTEM_PROMPT = """
            너는 한국 부동산 서비스의 AI 상담사다.
            한국어로 답한다.
            매물 데이터, 위험도 점수, 계약 문서 근거가 제공되지 않은 내용은 단정하지 않는다.
            정책, 법률, 계약 안전성은 일반 정보로만 안내하고 전문가 확인이 필요하다고 말한다.
            마크다운 문법(**, ##, -, ``` 등)을 사용하지 않는다. 일반 텍스트로만 답변한다.
            """;

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;

    @Value("${app.ai.enabled:false}")
    private boolean enabled;

    public String answer(String message) {
        if (!enabled) {
            return """
                    아직 AI 답변 생성이 비활성화되어 있어 기본 안내만 제공합니다.
                    일반 부동산 질문은 답변 가능하지만, 정책/계약/법률처럼 정확한 근거가 필요한 내용은 RAG 문서 연결 후 더 안전하게 답변할 수 있습니다.
                    """.strip();
        }

        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            return "AI 답변 생성 설정이 아직 준비되지 않았습니다.";
        }

        try {
            return builder.defaultSystem(SYSTEM_PROMPT)
                    .build()
                    .prompt()
                    .user(message)
                    .call()
                    .content();
        } catch (RuntimeException e) {
            return "AI 답변 생성 중 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.";
        }
    }
}
