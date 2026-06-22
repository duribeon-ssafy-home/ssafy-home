package com.ssafy.home.ai.controller;

import com.ssafy.home.ai.dto.request.AiCompareRequest;
import com.ssafy.home.ai.dto.response.AiCompareResponse;
import com.ssafy.home.ai.service.AiCompareService;
import com.ssafy.home.common.annotation.CurrentUser;
import com.ssafy.home.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI Compare", description = "AI property comparison analysis API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/ai")
public class AiCompareController {

    private final AiCompareService aiCompareService;

    @Operation(
            summary = "Compare selected properties with AI-ready analysis",
            description = "Analyzes 2 to 4 selected properties using property, risk, and lifestyle data."
    )
    @PostMapping("/compare")
    public ApiResponse<AiCompareResponse> compare(
            @Valid @RequestBody AiCompareRequest request,
            @Parameter(hidden = true) @CurrentUser Long userId
    ) {
        return ApiResponse.success(
                "AI comparison analysis completed.",
                aiCompareService.compare(request, userId)
        );
    }
}
