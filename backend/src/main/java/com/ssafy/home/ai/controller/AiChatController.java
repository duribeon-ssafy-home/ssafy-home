package com.ssafy.home.ai.controller;

import com.ssafy.home.ai.dto.request.AiChatRequest;
import com.ssafy.home.ai.dto.response.AiChatResponse;
import com.ssafy.home.ai.service.AiOrchestratorService;
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

@Tag(name = "AI Chat", description = "AI intent routing chat API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private final AiOrchestratorService aiOrchestratorService;

    @Operation(
            summary = "Route an AI chat request",
            description = "Routes the user question to compare, RAG, risk explanation, or general real-estate answer flow."
    )
    @PostMapping("/chat")
    public ApiResponse<AiChatResponse> chat(
            @Valid @RequestBody AiChatRequest request,
            @Parameter(hidden = true) @CurrentUser Long userId
    ) {
        return ApiResponse.success("AI chat request completed.", aiOrchestratorService.chat(request, userId));
    }
}
