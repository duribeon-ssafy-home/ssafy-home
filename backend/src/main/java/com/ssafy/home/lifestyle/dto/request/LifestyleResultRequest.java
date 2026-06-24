package com.ssafy.home.lifestyle.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record LifestyleResultRequest(
        @NotEmpty
        @Size(min = 6, max = 6)
        List<@Valid LifestyleAnswerRequest> answers,

        @Positive
        Integer monthlyRentMax,

        @Positive
        Long depositMax
) {
}
