package com.ssafy.home.recommendation.service;

import com.ssafy.home.lifestyle.entity.LifestyleResult;
import com.ssafy.home.property.entity.AreaFacilityCount;
import com.ssafy.home.property.entity.Property;
import com.ssafy.home.property.entity.RoomType;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecommendationScorer {

    private final LifestyleRecommendationPolicy policy;

    public int calculate(LifestyleResult lifestyle, Property property, AreaFacilityCount area) {
        int activeWeight = 0;
        int matchedWeight = 0;

        if (lifestyle.getFacilityScoreMin() != null) {
            activeWeight += LifestyleRecommendationPolicy.FACILITY_SCORE_WEIGHT;
            if (area != null && area.coverageScore() >= lifestyle.getFacilityScoreMin()) {
                matchedWeight += LifestyleRecommendationPolicy.FACILITY_SCORE_WEIGHT;
            }
        }

        if (lifestyle.getFacilityCountMin() != null) {
            activeWeight += LifestyleRecommendationPolicy.FACILITY_COUNT_WEIGHT;
            if (area != null && area.totalCount() >= lifestyle.getFacilityCountMin()) {
                matchedWeight += LifestyleRecommendationPolicy.FACILITY_COUNT_WEIGHT;
            }
        }

        if (lifestyle.getMonthlyRentMax() != null) {
            activeWeight += LifestyleRecommendationPolicy.MONTHLY_RENT_WEIGHT;
            if (property.getMonthlyRent() != null
                    && property.getMonthlyRent() <= lifestyle.getMonthlyRentMax()) {
                matchedWeight += LifestyleRecommendationPolicy.MONTHLY_RENT_WEIGHT;
            }
        }

        if (lifestyle.getDepositMax() != null) {
            activeWeight += LifestyleRecommendationPolicy.DEPOSIT_WEIGHT;
            if (property.getDeposit() != null
                    && property.getDeposit() <= lifestyle.getDepositMax()) {
                matchedWeight += LifestyleRecommendationPolicy.DEPOSIT_WEIGHT;
            }
        }

        if (lifestyle.getAreaMin() != null) {
            activeWeight += LifestyleRecommendationPolicy.AREA_WEIGHT;
            if (isGreaterThanOrEqualTo(property.getArea(), lifestyle.getAreaMin())) {
                matchedWeight += LifestyleRecommendationPolicy.AREA_WEIGHT;
            }
        }

        if (lifestyle.getBuildYearMin() != null) {
            activeWeight += LifestyleRecommendationPolicy.BUILD_YEAR_WEIGHT;
            if (property.getBuildYear() != null
                    && property.getBuildYear() >= lifestyle.getBuildYearMin()) {
                matchedWeight += LifestyleRecommendationPolicy.BUILD_YEAR_WEIGHT;
            }
        }

        RoomType representativeRoomType = policy.representativeRoomType(lifestyle.getLifestyleType());
        if (representativeRoomType != null) {
            activeWeight += LifestyleRecommendationPolicy.ROOM_TYPE_WEIGHT;
            if (representativeRoomType == property.getRoomType()) {
                matchedWeight += LifestyleRecommendationPolicy.ROOM_TYPE_WEIGHT;
            }
        }

        if (activeWeight == 0) {
            return 0;
        }
        return Math.round(matchedWeight * 100.0f / activeWeight);
    }

    private boolean isGreaterThanOrEqualTo(BigDecimal actual, BigDecimal min) {
        return actual != null && actual.compareTo(min) >= 0;
    }
}
