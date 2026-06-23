package com.ssafy.home.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AiCompareRequest(
        @NotNull
        @Size(min = 2, max = 4)
        List<@NotNull Long> propertyIds,

        @NotBlank
        @Size(max = 500)
        String question
) {
}
