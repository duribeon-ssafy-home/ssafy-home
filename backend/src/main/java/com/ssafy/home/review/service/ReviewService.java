package com.ssafy.home.review.service;

import com.ssafy.home.common.exception.BusinessException;
import com.ssafy.home.common.exception.ErrorCode;
import com.ssafy.home.property.repository.PropertyRepository;
import com.ssafy.home.review.dto.ReviewRequest;
import com.ssafy.home.review.dto.ReviewResponse;
import com.ssafy.home.review.entity.PropertyReview;
import com.ssafy.home.review.repository.PropertyReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class ReviewService {

    private final PropertyReviewRepository reviewRepository;
    private final PropertyRepository propertyRepository;

    public Page<ReviewResponse> getReviews(Long propertyId, Pageable pageable) {
        validatePropertyExists(propertyId);
        return reviewRepository.findAllByPropertyId(propertyId, pageable)
                .map(ReviewResponse::from);
    }

    @Transactional
    public ReviewResponse createReview(Long propertyId, ReviewRequest request) {
        validatePropertyExists(propertyId);
        PropertyReview review = PropertyReview.builder()
                .propertyId(propertyId)
                .nickname(request.nickname().trim())
                .content(request.content().trim())
                .build();
        return ReviewResponse.from(reviewRepository.save(review));
    }

    private void validatePropertyExists(Long propertyId) {
        if (!propertyRepository.existsById(propertyId)) {
            throw new BusinessException(ErrorCode.PROPERTY_NOT_FOUND);
        }
    }
}
