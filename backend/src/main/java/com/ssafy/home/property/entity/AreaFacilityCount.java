package com.ssafy.home.property.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "area_facility_counts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AreaFacilityCount {

    private static final int FACILITY_CATEGORY_COUNT = 7;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long areaId;

    @Column(nullable = false, length = 50)
    private String sido;

    @Column(nullable = false, length = 50)
    private String gugun;

    @Column(nullable = false, length = 50)
    private String dong;

    private BigDecimal centerLat;
    private BigDecimal centerLng;

    @Column(name = "subway_count_500m")
    private Integer subwayCount500m;

    @Column(name = "mart_count_1km")
    private Integer martCount1km;

    @Column(name = "convenience_count_500m")
    private Integer convenienceCount500m;

    @Column(name = "hospital_count_1km")
    private Integer hospitalCount1km;

    @Column(name = "pharmacy_count_500m")
    private Integer pharmacyCount500m;

    @Column(name = "cafe_count_500m")
    private Integer cafeCount500m;

    @Column(name = "restaurant_count_500m")
    private Integer restaurantCount500m;

    private LocalDateTime calculatedAt;

    public int totalCount() {
        return coalesce(subwayCount500m)
                + coalesce(martCount1km)
                + coalesce(convenienceCount500m)
                + coalesce(hospitalCount1km)
                + coalesce(pharmacyCount500m)
                + coalesce(cafeCount500m)
                + coalesce(restaurantCount500m);
    }

    public int coverageScore() {
        int coveredCategories = covered(subwayCount500m)
                + covered(martCount1km)
                + covered(convenienceCount500m)
                + covered(hospitalCount1km)
                + covered(pharmacyCount500m)
                + covered(cafeCount500m)
                + covered(restaurantCount500m);

        return Math.round(coveredCategories * 100.0f / FACILITY_CATEGORY_COUNT);
    }

    private int coalesce(Integer value) {
        return value != null ? value : 0;
    }

    private int covered(Integer value) {
        return value != null && value > 0 ? 1 : 0;
    }
}
