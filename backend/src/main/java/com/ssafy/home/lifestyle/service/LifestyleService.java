package com.ssafy.home.lifestyle.service;

import com.ssafy.home.common.exception.BusinessException;
import com.ssafy.home.common.exception.ErrorCode;
import com.ssafy.home.lifestyle.dto.request.LifestyleAnswerRequest;
import com.ssafy.home.lifestyle.dto.request.LifestyleResultRequest;
import com.ssafy.home.lifestyle.dto.response.LifestyleFilterPresetResponse;
import com.ssafy.home.lifestyle.dto.response.LifestyleQuestionResponse;
import com.ssafy.home.lifestyle.dto.response.LifestyleResultResponse;
import com.ssafy.home.lifestyle.entity.LifestyleResult;
import com.ssafy.home.lifestyle.repository.LifestyleResultRepository;
import com.ssafy.home.lifestyle.type.LifestyleCategory;
import com.ssafy.home.lifestyle.type.LifestyleQuestion;
import com.ssafy.home.lifestyle.type.LifestyleType;
import com.ssafy.home.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class LifestyleService {

    private static final String OPTION_A = "A";
    private static final String OPTION_B = "B";
    private static final int FACILITY_SCORE_MIN = 70;
    private static final int FACILITY_COUNT_MIN = 20;
    private static final int MONTHLY_RENT_MAX = 50;
    private static final long DEPOSIT_MAX = 1000L;
    private static final BigDecimal AREA_MIN = BigDecimal.valueOf(25);
    private static final int BUILD_YEAR_MIN = 2016;

    private final LifestyleResultRepository lifestyleResultRepository;
    private final UserRepository userRepository;

    public List<LifestyleQuestionResponse> getQuestions() {
        return Arrays.stream(LifestyleQuestion.values())
                .map(LifestyleQuestionResponse::from)
                .toList();
    }

    public LifestyleResultResponse previewResult(LifestyleResultRequest request) {
        LifestyleAnalysis analysis = analyze(request);
        return toResponse(analysis);
    }

    @Transactional
    public LifestyleResultResponse saveResult(LifestyleResultRequest request, Long userId) {
        validateUserExists(userId);
        LifestyleAnalysis analysis = analyze(request);

        LifestyleResult result = lifestyleResultRepository.save(LifestyleResult.builder()
                .userId(userId)
                .lifestyleType(analysis.lifestyleType())
                .livingConvenienceScore(analysis.livingConvenienceScore())
                .costSensitivityScore(analysis.costSensitivityScore())
                .homeQualityScore(analysis.homeQualityScore())
                .facilityScoreMin(analysis.facilityScoreMin())
                .facilityCountMin(analysis.facilityCountMin())
                .monthlyRentMax(analysis.monthlyRentMax())
                .depositMax(analysis.depositMax())
                .areaMin(analysis.areaMin())
                .buildYearMin(analysis.buildYearMin())
                .build());

        return LifestyleResultResponse.from(result);
    }

    public LifestyleResultResponse getMyLatestResult(Long userId) {
        validateUserExists(userId);
        LifestyleResult result = lifestyleResultRepository.findTopByUserIdOrderByCreatedAtDescIdDesc(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIFESTYLE_RESULT_NOT_FOUND));
        return LifestyleResultResponse.from(result);
    }

    private LifestyleAnalysis analyze(LifestyleResultRequest request) {
        Map<Integer, String> answers = validateAnswers(request.answers());

        int livingConvenienceScore = calculateScore(answers, LifestyleCategory.LIVING_CONVENIENCE);
        int costSensitivityScore = calculateScore(answers, LifestyleCategory.COST_SENSITIVITY);
        int homeQualityScore = calculateScore(answers, LifestyleCategory.HOME_QUALITY);

        LifestyleType lifestyleType = LifestyleType.from(
                livingConvenienceScore > 0,
                costSensitivityScore > 0,
                homeQualityScore > 0
        );

        return new LifestyleAnalysis(
                lifestyleType,
                livingConvenienceScore,
                costSensitivityScore,
                homeQualityScore,
                createFacilityScoreMin(answers),
                createFacilityCountMin(answers),
                resolveMonthlyRentMax(request, answers),
                resolveDepositMax(request, answers),
                isSelectedA(answers, LifestyleQuestion.AREA) ? AREA_MIN : null,
                isSelectedA(answers, LifestyleQuestion.BUILD_YEAR) ? BUILD_YEAR_MIN : null
        );
    }

    private LifestyleResultResponse toResponse(LifestyleAnalysis analysis) {
        return new LifestyleResultResponse(
                analysis.lifestyleType(),
                analysis.lifestyleType().getTypeName(),
                new LifestyleFilterPresetResponse(
                        analysis.facilityScoreMin(),
                        analysis.facilityCountMin(),
                        analysis.monthlyRentMax(),
                        analysis.depositMax(),
                        analysis.areaMin(),
                        analysis.buildYearMin()
                )
        );
    }

    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private Map<Integer, String> validateAnswers(List<LifestyleAnswerRequest> answerRequests) {
        Map<Integer, String> answers = new LinkedHashMap<>();

        for (LifestyleAnswerRequest answerRequest : answerRequests) {
            int questionId = answerRequest.questionId();
            LifestyleQuestion.findByQuestionId(questionId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));

            String selectedOption = answerRequest.selectedOption().trim().toUpperCase(Locale.ROOT);
            if (!OPTION_A.equals(selectedOption) && !OPTION_B.equals(selectedOption)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }
            if (answers.put(questionId, selectedOption) != null) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }
        }

        if (answers.size() != LifestyleQuestion.values().length) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        return answers;
    }

    private int calculateScore(Map<Integer, String> answers, LifestyleCategory category) {
        return (int) Arrays.stream(LifestyleQuestion.values())
                .filter(question -> question.getCategory() == category)
                .filter(question -> isSelectedA(answers, question))
                .count();
    }

    private Integer createFacilityScoreMin(Map<Integer, String> answers) {
        if (isSelectedA(answers, LifestyleQuestion.DAILY_LIVING)
                || isSelectedA(answers, LifestyleQuestion.NEIGHBORHOOD_FACILITIES)) {
            return FACILITY_SCORE_MIN;
        }
        return null;
    }

    private Integer createFacilityCountMin(Map<Integer, String> answers) {
        if (isSelectedA(answers, LifestyleQuestion.NEIGHBORHOOD_FACILITIES)) {
            return FACILITY_COUNT_MIN;
        }
        return null;
    }

    private Integer resolveMonthlyRentMax(LifestyleResultRequest request, Map<Integer, String> answers) {
        if (request.monthlyRentMax() != null) {
            return request.monthlyRentMax();
        }
        return isSelectedA(answers, LifestyleQuestion.MONTHLY_RENT) ? MONTHLY_RENT_MAX : null;
    }

    private Long resolveDepositMax(LifestyleResultRequest request, Map<Integer, String> answers) {
        if (request.depositMax() != null) {
            return request.depositMax();
        }
        return isSelectedA(answers, LifestyleQuestion.DEPOSIT) ? DEPOSIT_MAX : null;
    }

    private boolean isSelectedA(Map<Integer, String> answers, LifestyleQuestion question) {
        return OPTION_A.equals(answers.get(question.getQuestionId()));
    }

    private record LifestyleAnalysis(
            LifestyleType lifestyleType,
            int livingConvenienceScore,
            int costSensitivityScore,
            int homeQualityScore,
            Integer facilityScoreMin,
            Integer facilityCountMin,
            Integer monthlyRentMax,
            Long depositMax,
            BigDecimal areaMin,
            Integer buildYearMin
    ) {
    }
}
