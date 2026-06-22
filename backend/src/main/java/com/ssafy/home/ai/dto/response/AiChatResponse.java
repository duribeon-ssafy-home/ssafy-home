package com.ssafy.home.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ssafy.home.ai.type.AiIntent;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiChatResponse(
        AiIntent intent,
        String answer,
        AiCompareResponse compare,
        List<AiCompareSourceResponse> sources,
        boolean verified,
        List<String> warnings
) {
}
