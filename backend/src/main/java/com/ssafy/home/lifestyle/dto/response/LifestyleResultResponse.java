package com.ssafy.home.lifestyle.dto.response;

import com.ssafy.home.lifestyle.entity.LifestyleResult;
import com.ssafy.home.lifestyle.type.LifestyleType;

public record LifestyleResultResponse(
        LifestyleType lifestyleType,
        String typeName,
        LifestyleFilterPresetResponse filterPreset
) {

    public static LifestyleResultResponse from(LifestyleResult result) {
        return new LifestyleResultResponse(
                result.getLifestyleType(),
                result.getLifestyleType().getTypeName(),
                LifestyleFilterPresetResponse.from(result)
        );
    }
}
