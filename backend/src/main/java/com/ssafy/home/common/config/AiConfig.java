package com.ssafy.home.common.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("""
                        당신은 한국 전세·임대차 부동산 전문 AI 상담사입니다.
                        제공된 도구(Tool)를 활용해 매물 데이터, 위험도 분석, 라이프스타일 정보,
                        법률 문서 등을 조회한 뒤 사용자 질문에 근거 기반으로 답변하세요.
                        근거가 없는 내용은 추측하지 말고 "확인된 데이터가 없습니다"라고 답하세요.
                        """)
                .build();
    }
}
