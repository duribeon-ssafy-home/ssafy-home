package com.ssafy.home.risk.dto.response;

import com.ssafy.home.risk.type.RiskLabel;

public record RiskResponse(
        Long propertyId,
        RiskLabel label,
        int score,
        Long marketPriceAvg,
        Double priceGapRate,
        int reportCount,
        boolean ownerVerified,
        Long avgDeposit,
        Long avgMonthlyRent
) {
    public static RiskResponse unknown(Long propertyId, int reportCount, boolean ownerVerified) {
        return new RiskResponse(propertyId, RiskLabel.UNKNOWN, 0, null, null, reportCount, ownerVerified, null, null);
    }
}
