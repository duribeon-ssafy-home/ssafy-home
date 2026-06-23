package com.ssafy.home.ai.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.home.risk.service.RiskService;
import org.springframework.ai.tool.annotation.Tool;

public class RiskTool {

    private final RiskService riskService;
    private final ObjectMapper objectMapper;

    public RiskTool(RiskService riskService, ObjectMapper objectMapper) {
        this.riskService = riskService;
        this.objectMapper = objectMapper;
    }

    @Tool(description = "매물 ID로 전세사기 위험도 분석 결과(위험 등급, 점수, 시세 대비 차이율, 신고 수)를 조회합니다.")
    public String analyzePropertyRisk(Long propertyId) {
        try {
            var response = riskService.analyzeRisk(propertyId);
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            return "매물 ID " + propertyId + " 위험도 조회 실패: " + e.getMessage();
        }
    }
}
