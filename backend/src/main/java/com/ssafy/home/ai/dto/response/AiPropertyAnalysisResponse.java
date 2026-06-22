package com.ssafy.home.ai.dto.response;

import com.ssafy.home.risk.type.RiskLabel;
import java.util.List;

public record AiPropertyAnalysisResponse(
        Long propertyId,
        String title,
        RiskLabel riskLabel,
        String riskLabelText,
        int riskScore,
        Integer lifestyleFitScore,
        int costScore,
        int totalScore,
        List<String> pros,
        List<String> cons
) {
}
