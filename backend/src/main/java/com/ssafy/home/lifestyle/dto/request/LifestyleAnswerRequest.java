package com.ssafy.home.lifestyle.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LifestyleAnswerRequest(
        @NotNull Integer questionId,
        @NotBlank String selectedOption
) {
}
