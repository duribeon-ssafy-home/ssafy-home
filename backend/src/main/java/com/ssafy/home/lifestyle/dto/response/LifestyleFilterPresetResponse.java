package com.ssafy.home.lifestyle.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ssafy.home.lifestyle.entity.LifestyleResult;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LifestyleFilterPresetResponse(
        Integer facilityScoreMin,
        Integer facilityCountMin,
        Integer monthlyRentMax,
        Long depositMax,
        BigDecimal areaMin,
        Integer buildYearMin,
        String preferredSido,
        String preferredGugun,
        String preferredDong
) {

    public static LifestyleFilterPresetResponse from(LifestyleResult result) {
        return new LifestyleFilterPresetResponse(
                result.getFacilityScoreMin(),
                result.getFacilityCountMin(),
                result.getMonthlyRentMax(),
                result.getDepositMax(),
                result.getAreaMin(),
                result.getBuildYearMin(),
                result.getPreferredSido(),
                result.getPreferredGugun(),
                result.getPreferredDong()
        );
    }
}
