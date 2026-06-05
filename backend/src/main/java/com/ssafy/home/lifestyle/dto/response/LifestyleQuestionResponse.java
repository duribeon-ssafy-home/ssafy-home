package com.ssafy.home.lifestyle.dto.response;

import com.ssafy.home.lifestyle.type.LifestyleCategory;
import com.ssafy.home.lifestyle.type.LifestyleQuestion;

public record LifestyleQuestionResponse(
        int questionId,
        LifestyleCategory category,
        String title,
        String optionA,
        String optionB,
        String mapping
) {

    public static LifestyleQuestionResponse from(LifestyleQuestion question) {
        return new LifestyleQuestionResponse(
                question.getQuestionId(),
                question.getCategory(),
                question.getTitle(),
                question.getOptionA(),
                question.getOptionB(),
                question.getMapping()
        );
    }
}
