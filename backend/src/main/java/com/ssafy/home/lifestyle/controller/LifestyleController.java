package com.ssafy.home.lifestyle.controller;

import com.ssafy.home.common.annotation.CurrentUser;
import com.ssafy.home.common.response.ApiResponse;
import com.ssafy.home.lifestyle.dto.request.LifestyleResultRequest;
import com.ssafy.home.lifestyle.dto.response.LifestyleQuestionResponse;
import com.ssafy.home.lifestyle.dto.response.LifestyleResultResponse;
import com.ssafy.home.lifestyle.service.LifestyleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "생활 성향", description = "생활 성향 질문, 결과 저장, 최근 결과 조회 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/lifestyle")
public class LifestyleController {

    private final LifestyleService lifestyleService;

    @Operation(summary = "생활 성향 질문 조회", description = "생활 성향 설문 질문 6개와 A/B 선택지를 반환합니다.")
    @GetMapping("/questions")
    public ResponseEntity<ApiResponse<List<LifestyleQuestionResponse>>> getQuestions() {
        return ResponseEntity.ok(ApiResponse.success("생활 성향 질문을 조회했습니다.", lifestyleService.getQuestions()));
    }

    @Operation(summary = "생활 성향 결과 저장", description = "사용자의 답변을 저장하고 성향 유형과 매물 필터 프리셋을 반환합니다.")
    @PostMapping("/results")
    public ResponseEntity<ApiResponse<LifestyleResultResponse>> saveResult(
            @Valid @RequestBody LifestyleResultRequest request,
            @Parameter(hidden = true) @CurrentUser Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success("생활 성향 결과를 저장했습니다.", lifestyleService.saveResult(request, userId)));
    }

    @Operation(summary = "내 최근 생활 성향 결과 조회", description = "현재 로그인한 사용자의 최근 생활 성향 결과를 반환합니다.")
    @GetMapping("/results/me")
    public ResponseEntity<ApiResponse<LifestyleResultResponse>> getMyLatestResult(
            @Parameter(hidden = true) @CurrentUser Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success("최근 생활 성향 결과를 조회했습니다.", lifestyleService.getMyLatestResult(userId)));
    }
}
