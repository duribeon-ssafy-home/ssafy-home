package com.ssafy.home.risk.service;

import com.ssafy.home.common.exception.BusinessException;
import com.ssafy.home.common.exception.ErrorCode;
import com.ssafy.home.property.entity.DataSource;
import com.ssafy.home.property.entity.Property;
import com.ssafy.home.property.entity.PropertyStatus;
import com.ssafy.home.property.entity.RentType;
import com.ssafy.home.property.repository.RentalPriceRow;
import com.ssafy.home.property.repository.PropertyRepository;
import com.ssafy.home.report.entity.Report;
import com.ssafy.home.report.repository.ReportRepository;
import com.ssafy.home.risk.dto.response.RiskResponse;
import com.ssafy.home.risk.type.RiskLabel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RiskService {

    private static final int NEARBY_MIN_COUNT = 3;
    // 전월세전환율 5% 기준: 월세 × 12개월 / 0.05 = 월세 × 240 → 환산전세금
    private static final long JEONSE_CONVERSION_MULTIPLIER = 240L;

    private final PropertyRepository propertyRepository;
    private final ReportRepository reportRepository;

    public RiskResponse analyzeRisk(Long propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROPERTY_NOT_FOUND));

        if (property.getStatus() != PropertyStatus.APPROVED) {
            throw new BusinessException(ErrorCode.PROPERTY_NOT_FOUND);
        }

        List<Report> reports = reportRepository.findAllByProperty_PropertyId(propertyId);
        boolean ownerVerified = property.getDataSource() == DataSource.AGENT;

        if (property.getArea() == null || property.getRentType() == null) {
            return RiskResponse.unknown(propertyId, reports.size(), ownerVerified);
        }

        BigDecimal areaMin = property.getArea().subtract(BigDecimal.valueOf(20));
        BigDecimal areaMax = property.getArea().add(BigDecimal.valueOf(20));

        double marketAvg;
        double priceGapRate;
        Long avgDeposit = null;
        Long avgMonthlyRent = null;

        if (property.getRentType() == RentType.JEONSE) {
            // 전세: 전세금(deposit)만 비교
            if (property.getDeposit() == null) {
                return RiskResponse.unknown(propertyId, reports.size(), ownerVerified);
            }
            List<Long> nearbyDeposits = propertyRepository.findNearbyDeposits(
                    property.getDong(), RentType.JEONSE,
                    areaMin, areaMax, PropertyStatus.APPROVED, propertyId);
            if (nearbyDeposits.size() < NEARBY_MIN_COUNT) {
                return RiskResponse.unknown(propertyId, reports.size(), ownerVerified);
            }
            marketAvg = nearbyDeposits.stream().mapToLong(Long::longValue).average().orElseThrow();
            priceGapRate = (property.getDeposit() - marketAvg) / marketAvg * 100.0;
        } else {
            // 월세 / 반전세: 환산전세금 = 보증금 + 월세 × 240 으로 비교
            if (property.getDeposit() == null || property.getMonthlyRent() == null) {
                return RiskResponse.unknown(propertyId, reports.size(), ownerVerified);
            }
            List<RentalPriceRow> nearbyRentals = propertyRepository.findNearbyRentals(
                    property.getDong(), property.getRentType(),
                    areaMin, areaMax, PropertyStatus.APPROVED, propertyId);
            if (nearbyRentals.size() < NEARBY_MIN_COUNT) {
                return RiskResponse.unknown(propertyId, reports.size(), ownerVerified);
            }
            marketAvg = nearbyRentals.stream()
                    .mapToLong(r -> r.getDeposit() + r.getMonthlyRent() * JEONSE_CONVERSION_MULTIPLIER)
                    .average().orElseThrow();
            long propertyEquiv = property.getDeposit() + property.getMonthlyRent() * JEONSE_CONVERSION_MULTIPLIER;
            priceGapRate = (propertyEquiv - marketAvg) / marketAvg * 100.0;
            avgDeposit = Math.round(nearbyRentals.stream().mapToLong(RentalPriceRow::getDeposit).average().orElseThrow());
            avgMonthlyRent = Math.round(nearbyRentals.stream().mapToLong(RentalPriceRow::getMonthlyRent).average().orElseThrow());
        }

        int priceScore = calcPriceScore(priceGapRate);
        int reportScore = Math.min(calcReportScore(reports), 40);
        int ownerPenalty = ownerVerified ? 0 : 10;
        int totalScore = Math.min(priceScore + reportScore + ownerPenalty, 100);

        return new RiskResponse(
                propertyId,
                determineLabel(totalScore),
                totalScore,
                Math.round(marketAvg),
                Math.round(priceGapRate * 10.0) / 10.0,
                reports.size(),
                ownerVerified,
                avgDeposit,
                avgMonthlyRent
        );
    }

    private int calcPriceScore(double priceGapRate) {
        if (priceGapRate < 10) return 0;
        if (priceGapRate < 20) return 15;
        if (priceGapRate < 35) return 30;
        return 50;
    }

    private int calcReportScore(List<Report> reports) {
        return reports.stream()
                .mapToInt(r -> switch (r.getReason()) {
                    case FRAUD_SUSPECTED, FAKE_LISTING -> 15;
                    case PRICE_MISMATCH, NO_CONTACT -> 8;
                    case PHOTO_MISMATCH, ETC -> 4;
                })
                .sum();
    }

    private RiskLabel determineLabel(int score) {
        if (score <= 30) return RiskLabel.SAFE;
        if (score <= 60) return RiskLabel.CAUTION;
        return RiskLabel.DANGER;
    }
}
