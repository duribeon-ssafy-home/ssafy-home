package com.ssafy.home.ai.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.home.lifestyle.service.LifestyleService;
import org.springframework.ai.tool.annotation.Tool;

public class LifestyleTool {

    private final LifestyleService lifestyleService;
    private final ObjectMapper objectMapper;
    private final Long userId;

    public LifestyleTool(LifestyleService lifestyleService, ObjectMapper objectMapper, Long userId) {
        this.lifestyleService = lifestyleService;
        this.objectMapper = objectMapper;
        this.userId = userId;
    }

    @Tool(description = "현재 로그인한 사용자의 라이프스타일 분석 결과(생활 유형, 우선순위 시설 등)를 조회합니다.")
    public String getUserLifestyle() {
        try {
            var response = lifestyleService.getMyLatestResult(userId);
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            return "라이프스타일 정보 없음: 사용자가 라이프스타일 설문을 완료하지 않았습니다.";
        }
    }
}
