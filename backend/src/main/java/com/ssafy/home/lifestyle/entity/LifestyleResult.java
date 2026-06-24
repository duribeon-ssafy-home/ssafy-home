package com.ssafy.home.lifestyle.entity;

import com.ssafy.home.common.entity.BaseTimeEntity;
import com.ssafy.home.lifestyle.type.LifestyleType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "lifestyle_results")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LifestyleResult extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lifestyle_result_id")
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LifestyleType lifestyleType;

    @Column(nullable = false)
    private int livingConvenienceScore;

    @Column(nullable = false)
    private int costSensitivityScore;

    @Column(nullable = false)
    private int homeQualityScore;

    private Integer facilityScoreMin;

    private Integer facilityCountMin;

    private Integer monthlyRentMax;

    private Long depositMax;

    private BigDecimal areaMin;

    private Integer buildYearMin;

    @Column(length = 50)
    private String preferredSido;

    @Column(length = 50)
    private String preferredGugun;

    @Column(length = 50)
    private String preferredDong;

    @Builder
    private LifestyleResult(
            Long userId,
            LifestyleType lifestyleType,
            int livingConvenienceScore,
            int costSensitivityScore,
            int homeQualityScore,
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
        this.userId = userId;
        this.lifestyleType = lifestyleType;
        this.livingConvenienceScore = livingConvenienceScore;
        this.costSensitivityScore = costSensitivityScore;
        this.homeQualityScore = homeQualityScore;
        this.facilityScoreMin = facilityScoreMin;
        this.facilityCountMin = facilityCountMin;
        this.monthlyRentMax = monthlyRentMax;
        this.depositMax = depositMax;
        this.areaMin = areaMin;
        this.buildYearMin = buildYearMin;
        this.preferredSido = preferredSido;
        this.preferredGugun = preferredGugun;
        this.preferredDong = preferredDong;
    }
}
