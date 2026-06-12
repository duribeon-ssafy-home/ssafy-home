package com.ssafy.home.lifestyle.type;

import java.util.Arrays;
import lombok.Getter;

@Getter
public enum LifestyleType {
    LIVING_COST_HOME_BALANCED(true, true, true, "균형 잡힌 생활권 실속형"),
    LIVING_COST_COMPACT(true, true, false, "생활권 중심 실속형"),
    LIVING_FLEXIBLE_HOME(true, false, true, "편안한 공간 우선형"),
    LIVING_FLEXIBLE_COMPACT(true, false, false, "생활권 중심 실용형"),
    LOCATION_FLEXIBLE_COST_HOME(false, true, true, "조건 꼼꼼 공간형"),
    LOCATION_FLEXIBLE_COST_COMPACT(false, true, false, "비용 절약 실속형"),
    LOCATION_FLEXIBLE_HOME(false, false, true, "집 자체 만족형"),
    LOCATION_FLEXIBLE_COMPACT(false, false, false, "조건 유연 탐색형");

    private final boolean livingConvenienceImportant;
    private final boolean costSensitive;
    private final boolean homeQualityImportant;
    private final String typeName;

    LifestyleType(
            boolean livingConvenienceImportant,
            boolean costSensitive,
            boolean homeQualityImportant,
            String typeName
    ) {
        this.livingConvenienceImportant = livingConvenienceImportant;
        this.costSensitive = costSensitive;
        this.homeQualityImportant = homeQualityImportant;
        this.typeName = typeName;
    }

    public static LifestyleType from(
            boolean livingConvenienceImportant,
            boolean costSensitive,
            boolean homeQualityImportant
    ) {
        return Arrays.stream(values())
                .filter(type -> type.livingConvenienceImportant == livingConvenienceImportant)
                .filter(type -> type.costSensitive == costSensitive)
                .filter(type -> type.homeQualityImportant == homeQualityImportant)
                .findFirst()
                .orElseThrow();
    }
}
