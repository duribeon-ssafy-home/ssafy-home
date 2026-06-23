package com.ssafy.home.ai.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.home.property.service.PropertyService;
import org.springframework.ai.tool.annotation.Tool;

public class PropertyTool {

    private final PropertyService propertyService;
    private final ObjectMapper objectMapper;

    public PropertyTool(PropertyService propertyService, ObjectMapper objectMapper) {
        this.propertyService = propertyService;
        this.objectMapper = objectMapper;
    }

    @Tool(description = "매물 ID로 매물 상세 정보(주소, 면적, 보증금, 월세, 층수, 건축연도, 거래유형 등)를 조회합니다.")
    public String getPropertyDetail(Long propertyId) {
        try {
            var response = propertyService.getProperty(propertyId);
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            return "매물 ID " + propertyId + " 조회 실패: " + e.getMessage();
        }
    }
}
