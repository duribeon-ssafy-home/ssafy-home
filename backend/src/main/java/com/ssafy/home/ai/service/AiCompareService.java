package com.ssafy.home.ai.service;

import com.ssafy.home.ai.agent.FinalAnswerAgent;
import com.ssafy.home.ai.dto.request.AiCompareRequest;
import com.ssafy.home.ai.dto.response.AiCompareResponse;
import com.ssafy.home.ai.dto.response.AiCompareSourceResponse;
import com.ssafy.home.ai.dto.response.AiPropertyAnalysisResponse;
import com.ssafy.home.common.exception.BusinessException;
import com.ssafy.home.common.exception.ErrorCode;
import com.ssafy.home.lifestyle.dto.response.LifestyleFilterPresetResponse;
import com.ssafy.home.lifestyle.dto.response.LifestyleResultResponse;
import com.ssafy.home.lifestyle.repository.LifestyleResultRepository;
import com.ssafy.home.property.dto.FacilityInfo;
import com.ssafy.home.property.dto.PropertyResponse;
import com.ssafy.home.property.entity.RentType;
import com.ssafy.home.property.service.PropertyService;
import com.ssafy.home.risk.dto.response.RiskResponse;
import com.ssafy.home.risk.service.RiskService;
import com.ssafy.home.risk.type.RiskLabel;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class AiCompareService {

    private final PropertyService propertyService;
    private final RiskService riskService;
    private final LifestyleResultRepository lifestyleResultRepository;
    private final FinalAnswerAgent finalAnswerAgent;
    private final RagKnowledgeService ragKnowledgeService;

    public AiCompareResponse compare(AiCompareRequest request, Long userId) {
        validateDistinctIds(request.propertyIds());

        Optional<LifestyleResultResponse> lifestyle = findLifestyleSafely(userId);
        List<CompareTarget> targets = request.propertyIds().stream()
                .map(propertyId -> new CompareTarget(
                        propertyService.getProperty(propertyId),
                        riskService.analyzeRisk(propertyId)
                ))
                .toList();

        List<AiPropertyAnalysisResponse> analyses = targets.stream()
                .map(target -> analyze(target, lifestyle.map(LifestyleResultResponse::filterPreset).orElse(null), targets))
                .sorted(Comparator.comparingInt(AiPropertyAnalysisResponse::totalScore).reversed())
                .toList();

        AiPropertyAnalysisResponse recommended = analyses.get(0);
        RagKnowledgeService.RagContext ragContext = ragKnowledgeService.retrieve(createRagQuery(request.question()), 5);
        List<AiCompareSourceResponse> sources = createSources(targets, lifestyle.isPresent(), ragContext.sources());
        boolean lifestyleQuestion = isLifestyleQuestion(request.question());
        boolean requiresLifestyleSurvey = lifestyle.isEmpty() && lifestyleQuestion;
        String surveyGuideMessage = createSurveyGuideMessage(requiresLifestyleSurvey);
        String fallbackAnswer = createFallbackAnswer(
                recommended,
                request.question(),
                lifestyle,
                requiresLifestyleSurvey,
                ragContext
        );

        return new AiCompareResponse(
                finalAnswerAgent.generate(
                        request.question(),
                        lifestyle.orElse(null),
                        analyses,
                        sources,
                        ragContext.documents(),
                        fallbackAnswer
                ),
                recommended.propertyId(),
                recommended.title(),
                recommended.totalScore(),
                recommended.riskLabelText(),
                lifestyle.map(LifestyleResultResponse::typeName).orElse(null),
                lifestyle.isPresent(),
                requiresLifestyleSurvey,
                surveyGuideMessage,
                analyses,
                sources
        );
    }

    private Optional<LifestyleResultResponse> findLifestyleSafely(Long userId) {
        return lifestyleResultRepository.findTopByUserIdOrderByCreatedAtDescIdDesc(userId)
                .map(LifestyleResultResponse::from);
    }

    private void validateDistinctIds(List<Long> propertyIds) {
        Set<Long> distinctIds = new HashSet<>(propertyIds);
        if (distinctIds.size() != propertyIds.size()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private AiPropertyAnalysisResponse analyze(
            CompareTarget target,
            LifestyleFilterPresetResponse preset,
            List<CompareTarget> allTargets
    ) {
        PropertyResponse property = target.property();
        RiskResponse risk = target.risk();

        Integer lifestyleFitScore = preset != null ? calculateLifestyleFitScore(property, preset) : null;
        int costScore = calculateCostScore(property, allTargets);
        int riskSafetyScore = calculateRiskSafetyScore(risk);
        int totalScore = lifestyleFitScore != null
                ? Math.round(lifestyleFitScore * 0.45f + riskSafetyScore * 0.40f + costScore * 0.15f)
                : Math.round(riskSafetyScore * 0.70f + costScore * 0.30f);

        return new AiPropertyAnalysisResponse(
                property.propertyId(),
                property.title(),
                risk.label(),
                riskLabelText(risk.label()),
                risk.score(),
                lifestyleFitScore,
                costScore,
                totalScore,
                createPros(property, risk, lifestyleFitScore, costScore),
                createCons(property, risk, lifestyleFitScore, costScore)
        );
    }

    private int calculateLifestyleFitScore(PropertyResponse property, LifestyleFilterPresetResponse preset) {
        int activeWeight = 0;
        int matchedWeight = 0;

        if (preset.facilityCountMin() != null) {
            activeWeight += 15;
            if (totalFacilityCount(property.facilityInfo()) >= preset.facilityCountMin()) {
                matchedWeight += 15;
            }
        }

        if (preset.monthlyRentMax() != null) {
            activeWeight += 20;
            if (property.rentType() == RentType.MONTHLY
                    && property.monthlyRent() != null
                    && property.monthlyRent() <= preset.monthlyRentMax()) {
                matchedWeight += 20;
            }
        }

        if (preset.depositMax() != null) {
            activeWeight += 20;
            if (property.deposit() != null && property.deposit() <= preset.depositMax()) {
                matchedWeight += 20;
            }
        }

        if (preset.areaMin() != null) {
            activeWeight += 20;
            if (isGreaterThanOrEqualTo(property.area(), preset.areaMin())) {
                matchedWeight += 20;
            }
        }

        if (preset.buildYearMin() != null) {
            activeWeight += 15;
            if (property.buildYear() != null && property.buildYear() >= preset.buildYearMin()) {
                matchedWeight += 15;
            }
        }

        if (activeWeight == 0) {
            return 50;
        }
        return Math.round(matchedWeight * 100.0f / activeWeight);
    }

    private int calculateCostScore(PropertyResponse property, List<CompareTarget> allTargets) {
        long maxDeposit = allTargets.stream()
                .map(CompareTarget::property)
                .map(PropertyResponse::deposit)
                .filter(value -> value != null && value > 0)
                .mapToLong(Long::longValue)
                .max()
                .orElse(0);

        int depositScore = 50;
        if (maxDeposit > 0 && property.deposit() != null) {
            depositScore = Math.round((maxDeposit - property.deposit()) * 100.0f / maxDeposit);
        }

        int rentScore = 50;
        int maxMonthlyRent = allTargets.stream()
                .map(CompareTarget::property)
                .filter(compareProperty -> compareProperty.rentType() == RentType.MONTHLY)
                .map(PropertyResponse::monthlyRent)
                .filter(value -> value != null && value > 0)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);

        if (property.rentType() == RentType.MONTHLY && maxMonthlyRent > 0 && property.monthlyRent() != null) {
            rentScore = Math.round((maxMonthlyRent - property.monthlyRent()) * 100.0f / maxMonthlyRent);
        }

        return Math.max(0, Math.min(100, Math.round(depositScore * 0.7f + rentScore * 0.3f)));
    }

    private int calculateRiskSafetyScore(RiskResponse risk) {
        if (risk.label() == RiskLabel.UNKNOWN) {
            return 50;
        }
        return Math.max(0, 100 - risk.score());
    }

    private List<String> createPros(PropertyResponse property, RiskResponse risk, Integer lifestyleFitScore, int costScore) {
        List<String> pros = new ArrayList<>();

        if (risk.label() == RiskLabel.SAFE) {
            pros.add("선택한 매물 중 위험 점수가 낮은 편입니다.");
        }
        if (risk.ownerVerified()) {
            pros.add("현재 데이터 기준으로 소유자 검증 정보가 확인됩니다.");
        }
        if (lifestyleFitScore != null && lifestyleFitScore >= 70) {
            pros.add("저장된 생활유형 조건과 잘 맞는 편입니다.");
        }
        if (costScore >= 70) {
            pros.add("이번 비교 목록 안에서 보증금과 월세 조건이 경쟁력 있습니다.");
        }
        if (property.area() != null) {
            pros.add("면적 정보가 있어 조건 비교에 활용할 수 있습니다.");
        }

        if (pros.isEmpty()) {
            pros.add("기본 매물 정보를 기준으로 비교할 수 있습니다.");
        }
        return pros;
    }

    private List<String> createCons(PropertyResponse property, RiskResponse risk, Integer lifestyleFitScore, int costScore) {
        List<String> cons = new ArrayList<>();

        if (risk.label() == RiskLabel.DANGER) {
            cons.add("위험 점수가 높은 편이므로 계약 전 확인을 우선해야 합니다.");
        } else if (risk.label() == RiskLabel.CAUTION) {
            cons.add("위험도가 주의 구간에 해당합니다.");
        } else if (risk.label() == RiskLabel.UNKNOWN) {
            cons.add("비교 가능한 데이터가 부족해 위험도를 충분히 계산하지 못했습니다.");
        }

        if (!risk.ownerVerified()) {
            cons.add("현재 데이터에서 소유자 검증 여부가 확인되지 않습니다.");
        }
        if (lifestyleFitScore != null && lifestyleFitScore < 50) {
            cons.add("저장된 생활유형 조건과 맞는 항목이 많지 않습니다.");
        }
        if (lifestyleFitScore == null) {
            cons.add("생활유형 설문 결과가 없어 생활패턴 기준 적합도는 계산하지 않았습니다.");
        }
        if (costScore < 40) {
            cons.add("이번 비교 목록 안에서 비용 조건이 가장 강한 편은 아닙니다.");
        }
        if (property.deposit() == null) {
            cons.add("보증금 정보가 누락되어 있습니다.");
        }

        if (cons.isEmpty()) {
            cons.add("현재 비교 데이터에서 큰 단점은 확인되지 않았습니다.");
        }
        return cons;
    }

    private String createFallbackAnswer(
            AiPropertyAnalysisResponse recommended,
            String question,
            Optional<LifestyleResultResponse> lifestyle,
            boolean requiresLifestyleSurvey,
            RagKnowledgeService.RagContext ragContext
    ) {
        String lifestyleLine = lifestyle
                .map(result -> "저장된 생활유형은 \"%s\"이며, 생활 적합도와 위험도, 비용 균형을 함께 반영했습니다."
                        .formatted(result.typeName()))
                .orElse("생활유형 설문 결과가 없어 위험도와 비용 조건을 중심으로 비교했습니다.");
        String surveyLine = requiresLifestyleSurvey
                ? "\n생활패턴 기준 추천을 더 정확히 받으려면 생활유형 설문을 먼저 완료해 주세요."
                : "";
        String ragLine = ragContext.available() && !ragContext.sources().isEmpty()
                ? "\n문서 근거 기준으로는 등기부등본, 선순위 권리, 보증금과 주변 시세, 보증보험 가능 여부도 함께 확인하는 것이 좋습니다."
                : "";

        return """
                현재 비교 데이터 기준으로는 "%s" 매물이 가장 먼저 검토하기 좋아 보입니다.
                총점은 %d점이고, 위험도는 %s입니다.
                %s%s%s
                """.formatted(
                recommended.title(),
                recommended.totalScore(),
                recommended.riskLabelText(),
                lifestyleLine,
                surveyLine,
                ragLine
        ).strip();
    }

    private List<AiCompareSourceResponse> createSources(
            List<CompareTarget> targets,
            boolean lifestyleAvailable,
            List<AiCompareSourceResponse> ragSources
    ) {
        List<AiCompareSourceResponse> sources = new ArrayList<>();
        if (lifestyleAvailable) {
            sources.add(new AiCompareSourceResponse("LIFESTYLE", "CURRENT_USER", "최신 생활유형 결과"));
        }

        for (CompareTarget target : targets) {
            String propertyId = String.valueOf(target.property().propertyId());
            sources.add(new AiCompareSourceResponse("PROPERTY", propertyId, target.property().title()));
            sources.add(new AiCompareSourceResponse("RISK", propertyId, "위험도 분석 결과"));
        }
        sources.addAll(ragSources);
        return sources;
    }

    private String createRagQuery(String question) {
        return """
                %s
                매물 비교 전세사기 위험도 보증금 주변 시세 등기부등본 계약 체크리스트 보증보험 대항력 확정일자
                """.formatted(question);
    }

    private boolean isLifestyleQuestion(String question) {
        String normalized = question.toLowerCase(Locale.ROOT);
        return normalized.contains("생활패턴")
                || normalized.contains("라이프스타일")
                || normalized.contains("생활유형")
                || normalized.contains("내 기준")
                || normalized.contains("나한테")
                || normalized.contains("출퇴근")
                || normalized.contains("편의시설")
                || normalized.contains("조용")
                || normalized.contains("넓")
                || normalized.contains("신축");
    }

    private String createSurveyGuideMessage(boolean requiresLifestyleSurvey) {
        if (!requiresLifestyleSurvey) {
            return null;
        }
        return "생활유형 설문을 완료하면 생활패턴 기준 추천을 더 정확하게 받을 수 있어요.";
    }

    private String riskLabelText(RiskLabel riskLabel) {
        return switch (riskLabel) {
            case SAFE -> "안전";
            case CAUTION -> "주의";
            case DANGER -> "위험";
            case UNKNOWN -> "분석 불가";
        };
    }

    private int totalFacilityCount(FacilityInfo facilityInfo) {
        if (facilityInfo == null) {
            return 0;
        }
        return safe(facilityInfo.subwayCount500m())
                + safe(facilityInfo.martCount1km())
                + safe(facilityInfo.convenienceCount500m())
                + safe(facilityInfo.hospitalCount1km())
                + safe(facilityInfo.pharmacyCount500m())
                + safe(facilityInfo.cafeCount500m())
                + safe(facilityInfo.restaurantCount500m());
    }

    private int safe(Integer value) {
        return value != null ? value : 0;
    }

    private boolean isGreaterThanOrEqualTo(BigDecimal actual, BigDecimal min) {
        return actual != null && actual.compareTo(min) >= 0;
    }

    private record CompareTarget(PropertyResponse property, RiskResponse risk) {
    }
}
