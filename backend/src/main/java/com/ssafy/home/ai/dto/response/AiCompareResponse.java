package com.ssafy.home.ai.dto.response;

import java.util.List;

public record AiCompareResponse(
        String answer,
        Long recommendedPropertyId,
        String recommendedTitle,
        int recommendedTotalScore,
        String recommendedRiskLabelText,
        String lifestyleTypeName,
        boolean lifestyleAvailable,
        boolean requiresLifestyleSurvey,
        String surveyGuideMessage,
        List<AiPropertyAnalysisResponse> propertyAnalyses,
        List<AiCompareSourceResponse> sources
) {
}
