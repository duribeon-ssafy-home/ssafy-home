package com.ssafy.home.property.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ssafy.home.property.entity.AreaFacilityCount;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FacilityInfo(
        Integer subwayCount500m,
        Integer martCount1km,
        Integer convenienceCount500m,
        Integer hospitalCount1km,
        Integer pharmacyCount500m,
        Integer cafeCount500m,
        Integer restaurantCount500m
) {
    public static FacilityInfo from(AreaFacilityCount area) {
        if (area == null) return null;
        return new FacilityInfo(
                area.getSubwayCount500m(),
                area.getMartCount1km(),
                area.getConvenienceCount500m(),
                area.getHospitalCount1km(),
                area.getPharmacyCount500m(),
                area.getCafeCount500m(),
                area.getRestaurantCount500m()
        );
    }
}
