package com.ssafy.home.recommendation.controller;

import com.ssafy.home.common.response.ApiResponse;
import com.ssafy.home.common.annotation.CurrentUser;
import com.ssafy.home.property.dto.PropertyResponse;
import com.ssafy.home.property.dto.PropertySearchCondition;
import com.ssafy.home.recommendation.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "추천", description = "자취 MBTI 기반 매물 추천 API")
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(
            summary = "맞춤 매물 추천",
            description = "사용자의 자취 MBTI(성향 테스트) 결과를 기반으로 조건에 맞는 매물 목록을 반환합니다. " +
                          "성향 테스트를 먼저 완료해야 합니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PropertyResponse>>> recommend(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @ParameterObject PropertySearchCondition condition,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PropertyResponse> result = recommendationService.recommend(userId, condition, pageable);
        return ResponseEntity.ok(ApiResponse.success("추천 매물 조회에 성공했습니다.", result));
    }
}
