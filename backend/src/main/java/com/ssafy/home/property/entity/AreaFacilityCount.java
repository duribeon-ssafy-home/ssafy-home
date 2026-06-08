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

    private Integer subwayCount500m;
    private Integer martCount1km;
    private Integer convenienceCount500m;
    private Integer hospitalCount1km;
    private Integer pharmacyCount500m;
    private Integer cafeCount500m;
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

    private int coalesce(Integer value) {
        return value != null ? value : 0;
    }
}
