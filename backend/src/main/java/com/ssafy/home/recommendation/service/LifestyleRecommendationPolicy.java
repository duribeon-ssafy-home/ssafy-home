package com.ssafy.home.recommendation.service;

import com.ssafy.home.lifestyle.type.LifestyleType;
import com.ssafy.home.property.entity.RoomType;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class LifestyleRecommendationPolicy {

    static final int FACILITY_SCORE_WEIGHT = 15;
    static final int FACILITY_COUNT_WEIGHT = 20;
    static final int MONTHLY_RENT_WEIGHT = 15;
    static final int DEPOSIT_WEIGHT = 15;
    static final int AREA_WEIGHT = 10;
    static final int BUILD_YEAR_WEIGHT = 10;
    static final int ROOM_TYPE_WEIGHT = 15;

    private final Map<LifestyleType, RoomType> representativeRoomTypes = new EnumMap<>(LifestyleType.class);

    public LifestyleRecommendationPolicy() {
        representativeRoomTypes.put(LifestyleType.LIVING_COST_HOME_BALANCED, RoomType.ONE_ROOM);
        representativeRoomTypes.put(LifestyleType.LIVING_COST_COMPACT, RoomType.ONE_ROOM);
        representativeRoomTypes.put(LifestyleType.LIVING_FLEXIBLE_HOME, RoomType.TWO_ROOM);
        representativeRoomTypes.put(LifestyleType.LIVING_FLEXIBLE_COMPACT, RoomType.ONE_ROOM);
        representativeRoomTypes.put(LifestyleType.LOCATION_FLEXIBLE_COST_HOME, RoomType.TWO_ROOM);
        representativeRoomTypes.put(LifestyleType.LOCATION_FLEXIBLE_COST_COMPACT, RoomType.ONE_ROOM);
        representativeRoomTypes.put(LifestyleType.LOCATION_FLEXIBLE_HOME, RoomType.TWO_ROOM);
        representativeRoomTypes.put(LifestyleType.LOCATION_FLEXIBLE_COMPACT, RoomType.ONE_ROOM);
    }

    public RoomType representativeRoomType(LifestyleType lifestyleType) {
        return representativeRoomTypes.get(lifestyleType);
    }
}
