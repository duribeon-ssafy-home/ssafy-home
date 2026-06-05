package com.ssafy.home.lifestyle.type;

import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;

@Getter
public enum LifestyleQuestion {
    DAILY_LIVING(
            1,
            LifestyleCategory.LIVING_CONVENIENCE,
            "생활권 중시도",
            "집 근처에서 장보기, 병원, 카페 같은 일상을 해결하고 싶어요",
            "필요한 곳은 조금 이동해서 가도 괜찮아요",
            "facilityScore"
    ),
    NEIGHBORHOOD_FACILITIES(
            2,
            LifestyleCategory.LIVING_CONVENIENCE,
            "생활권 중시도",
            "주변에 편의시설이 모여 있는 동네가 마음이 편해요",
            "동네 편의시설보다 집 조건이 더 중요해요",
            "facilityCount, facilityScore"
    ),
    MONTHLY_RENT(
            3,
            LifestyleCategory.COST_SENSITIVITY,
            "비용 민감도",
            "매달 나가는 월세 부담은 최대한 낮추고 싶어요",
            "마음에 드는 집이라면 월세는 어느 정도 감수할 수 있어요",
            "monthlyRentMax"
    ),
    DEPOSIT(
            4,
            LifestyleCategory.COST_SENSITIVITY,
            "비용 민감도",
            "처음 들어갈 때 드는 보증금은 낮을수록 좋아요",
            "보증금이 조금 높아도 조건이 괜찮으면 좋아요",
            "depositMax"
    ),
    AREA(
            5,
            LifestyleCategory.HOME_QUALITY,
            "집 자체 중시도",
            "집에서 편하게 지내려면 공간 여유가 중요해요",
            "공간이 작아도 필요한 조건만 맞으면 괜찮아요",
            "areaMin"
    ),
    BUILD_YEAR(
            6,
            LifestyleCategory.HOME_QUALITY,
            "집 자체 중시도",
            "오래 손보지 않아도 되는 깔끔한 집이 좋아요",
            "조금 오래된 집이어도 조건이 맞으면 괜찮아요",
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
