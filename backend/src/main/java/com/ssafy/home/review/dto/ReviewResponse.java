package com.ssafy.home.review.dto;

import com.ssafy.home.review.entity.PropertyReview;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long reviewId,
        String nickname,
        String content,
        LocalDateTime createdAt
) {
    public static ReviewResponse from(PropertyReview review) {
        return new ReviewResponse(
                review.getId(),
                review.getNickname(),
                review.getContent(),
                review.getCreatedAt()
        );
    }
}
