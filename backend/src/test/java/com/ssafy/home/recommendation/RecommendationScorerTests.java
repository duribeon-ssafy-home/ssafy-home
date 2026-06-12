package com.ssafy.home.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import com.ssafy.home.lifestyle.entity.LifestyleResult;
import com.ssafy.home.lifestyle.type.LifestyleType;
import com.ssafy.home.property.entity.AreaFacilityCount;
import com.ssafy.home.property.entity.Property;
import com.ssafy.home.property.entity.RoomType;
import com.ssafy.home.recommendation.service.LifestyleRecommendationPolicy;
import com.ssafy.home.recommendation.service.RecommendationScorer;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RecommendationScorerTests {

    private final RecommendationScorer scorer =
            new RecommendationScorer(new LifestyleRecommendationPolicy());

    @Test
    @DisplayName("활성화된 추천 조건을 모두 만족하면 100점을 반환한다")
    void calculateFullMatchScore() {
        LifestyleResult lifestyle = lifestyle(
                LifestyleType.LIVING_COST_HOME_BALANCED,
                70,
                20,
                50,
                1000L,
                BigDecimal.valueOf(25),
                2016
        );
        Property property = property(RoomType.ONE_ROOM, 45, 900L, "26.0", 2018);
        AreaFacilityCount area = area(3, 4, 5, 4, 4, 3, 2);

        int score = scorer.calculate(lifestyle, property, area);

        assertThat(score).isEqualTo(100);
    }

    @Test
    @DisplayName("설문 조건에 맞지 않는 매물은 제외하지 않고 낮은 점수만 받는다")
    void calculatePartialMatchScore() {
        LifestyleResult lifestyle = lifestyle(
                LifestyleType.LIVING_COST_HOME_BALANCED,
                70,
                20,
                50,
                1000L,
                BigDecimal.valueOf(25),
                2016
        );
        Property property = property(RoomType.ONE_ROOM, 70, 900L, "20.0", 2010);
        AreaFacilityCount area = area(0, 0, 0, 0, 0, 0, 0);

        int score = scorer.calculate(lifestyle, property, area);

        assertThat(score).isEqualTo(30);
    }

    @Test
    @DisplayName("facilityScoreMin은 시설 카테고리 커버리지 점수로 계산한다")
    void calculateFacilityCoverageScore() {
        LifestyleResult lifestyle = lifestyle(
                LifestyleType.LOCATION_FLEXIBLE_COMPACT,
                70,
                null,
                null,
                null,
                null,
                null
        );
        Property property = property(RoomType.ONE_ROOM, 70, 900L, "20.0", 2010);
        AreaFacilityCount fiveCategories = area(1, 1, 1, 1, 1, 0, 0);
        AreaFacilityCount fourCategories = area(1, 1, 1, 1, 0, 0, 0);

        int fiveCategoryScore = scorer.calculate(lifestyle, property, fiveCategories);
        int fourCategoryScore = scorer.calculate(lifestyle, property, fourCategories);

        assertThat(fiveCategories.coverageScore()).isEqualTo(71);
        assertThat(fourCategories.coverageScore()).isEqualTo(57);
        assertThat(fiveCategoryScore).isEqualTo(100);
        assertThat(fourCategoryScore).isEqualTo(50);
    }

    private LifestyleResult lifestyle(
            LifestyleType lifestyleType,
            Integer facilityScoreMin,
            Integer facilityCountMin,
            Integer monthlyRentMax,
            Long depositMax,
            BigDecimal areaMin,
            Integer buildYearMin
    ) {
        return LifestyleResult.builder()
                .userId(1L)
                .lifestyleType(lifestyleType)
                .livingConvenienceScore(0)
                .costSensitivityScore(0)
                .homeQualityScore(0)
                .facilityScoreMin(facilityScoreMin)
                .facilityCountMin(facilityCountMin)
                .monthlyRentMax(monthlyRentMax)
                .depositMax(depositMax)
                .areaMin(areaMin)
                .buildYearMin(buildYearMin)
                .build();
    }

    private Property property(
            RoomType roomType,
            Integer monthlyRent,
            Long deposit,
            String area,
            Integer buildYear
    ) {
        return Property.builder()
                .roomType(roomType)
                .monthlyRent(monthlyRent)
                .deposit(deposit)
                .area(new BigDecimal(area))
                .buildYear(buildYear)
                .build();
    }

    private AreaFacilityCount area(
            Integer subwayCount500m,
            Integer martCount1km,
            Integer convenienceCount500m,
            Integer hospitalCount1km,
            Integer pharmacyCount500m,
            Integer cafeCount500m,
            Integer restaurantCount500m
    ) {
        try {
            Constructor<AreaFacilityCount> constructor = AreaFacilityCount.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            AreaFacilityCount area = constructor.newInstance();
            ReflectionTestUtils.setField(area, "subwayCount500m", subwayCount500m);
            ReflectionTestUtils.setField(area, "martCount1km", martCount1km);
            ReflectionTestUtils.setField(area, "convenienceCount500m", convenienceCount500m);
            ReflectionTestUtils.setField(area, "hospitalCount1km", hospitalCount1km);
            ReflectionTestUtils.setField(area, "pharmacyCount500m", pharmacyCount500m);
            ReflectionTestUtils.setField(area, "cafeCount500m", cafeCount500m);
            ReflectionTestUtils.setField(area, "restaurantCount500m", restaurantCount500m);
            return area;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("테스트용 시설 카운트 객체를 만들 수 없습니다.", e);
        }
    }
}
