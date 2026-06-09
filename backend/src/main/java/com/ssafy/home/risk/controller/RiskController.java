package com.ssafy.home.risk.controller;

import com.ssafy.home.common.response.ApiResponse;
import com.ssafy.home.risk.dto.response.RiskResponse;
import com.ssafy.home.risk.service.RiskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "위험 분석", description = "매물 전세 사기 위험 분석 API")
@RequiredArgsConstructor
@RestController
public class RiskController {

    private final RiskService riskService;

    @Operation(summary = "매물 위험 분석 조회",
               description = "매물의 전세 사기 위험 라벨, 점수, 근거 데이터를 반환합니다. 인증 없이 공개 조회 가능합니다.")
    @GetMapping("/api/properties/{propertyId}/risk")
    public ApiResponse<RiskResponse> analyzeRisk(@PathVariable Long propertyId) {
        return ApiResponse.success("위험 분석 결과를 조회했습니다.", riskService.analyzeRisk(propertyId));
    }
}
