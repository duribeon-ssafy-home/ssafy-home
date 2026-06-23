package com.ssafy.home.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AiChatRequest(
        @NotBlank String question,
        @Size(max = 4) List<Long> propertyIds
) {}
