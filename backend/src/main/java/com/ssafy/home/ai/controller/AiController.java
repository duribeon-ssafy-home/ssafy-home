package com.ssafy.home.ai.controller;

import com.ssafy.home.ai.dto.request.AiChatRequest;
import com.ssafy.home.ai.dto.response.AiChatResponse;
import com.ssafy.home.ai.service.AiOrchestratorService;
import com.ssafy.home.auth.jwt.JwtAuthentication;
import com.ssafy.home.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiOrchestratorService aiOrchestratorService;

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<AiChatResponse>> chat(
            @Valid @RequestBody AiChatRequest request,
            @AuthenticationPrincipal JwtAuthentication auth
    ) {
        AiChatResponse response = aiOrchestratorService.chat(request, auth.userId());
        return ResponseEntity.ok(ApiResponse.success("AI 답변 생성 완료", response));
    }
}
