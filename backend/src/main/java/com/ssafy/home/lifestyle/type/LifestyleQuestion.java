package com.ssafy.home.lifestyle.type;

import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;

@Getter
public enum LifestyleQuestion {
    DAILY_LIVING(
            1,
            LifestyleCategory.LIVING_CONVENIENCE,
            "집 앞 생활",
            "편의점 정도는 슬리퍼 신고 다녀올 수 있어야 해요",
            "슬리퍼 거리보단 집 컨디션이 더 중요해요",
            "facilityScore"
    ),
    NEIGHBORHOOD_FACILITIES(
            2,
            LifestyleCategory.LIVING_CONVENIENCE,
            "동네 분위기",
            "생활 편의시설이 가까우면 괜히 든든해요",
            "조용하고 덜 복잡한 동네가 더 편해요",
            "facilityCount, facilityScore"
    ),
    MONTHLY_RENT(
            3,
            LifestyleCategory.COST_SENSITIVITY,
            "월세",
            "월세는 낮을수록 마음이 편해요",
            "마음에 들면 월세는 어느 정도 타협 가능해요",
            "monthlyRentMax"
    ),
    DEPOSIT(
            4,
            LifestyleCategory.COST_SENSITIVITY,
            "보증금",
            "보증금에 목돈이 많이 묶이는 건 피하고 싶어요",
            "조건만 좋다면 보증금은 조금 높아도 괜찮아요",
            "depositMax"
    ),
    AREA(
            5,
            LifestyleCategory.HOME_QUALITY,
            "집 크기",
            "집에서는 누울 자리 말고 숨 쉴 자리도 필요해요",
            "작아도 깔끔하고 실용적이면 충분해요",
            "areaMin"
    ),
    BUILD_YEAR(
            6,
            LifestyleCategory.HOME_QUALITY,
            "집 상태",
            "오래 손 안 봐도 되는 깔끔한 집이 좋아요",
            "조금 낡아도 가성비가 좋으면 괜찮아요",
            "buildYearMin"
    );

    private final int questionId;
    private final LifestyleCategory category;
    private final String title;
    private final String optionA;
    private final String optionB;
    private final String mapping;

    LifestyleQuestion(
            int questionId,
            LifestyleCategory category,
            String title,
            String optionA,
            String optionB,
            String mapping
    ) {
        this.questionId = questionId;
        this.category = category;
        this.title = title;
        this.optionA = optionA;
        this.optionB = optionB;
        this.mapping = mapping;
    }

    public static Optional<LifestyleQuestion> findByQuestionId(int questionId) {
        return Arrays.stream(values())
                .filter(question -> question.questionId == questionId)
                .findFirst();
    }
}
