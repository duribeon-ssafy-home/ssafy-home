package com.ssafy.home.review.controller;

import com.ssafy.home.common.response.ApiResponse;
import com.ssafy.home.review.dto.ReviewRequest;
import com.ssafy.home.review.dto.ReviewResponse;
import com.ssafy.home.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "후기", description = "매물 후기 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/properties/{propertyId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "후기 목록 조회")
    @GetMapping
    public ApiResponse<Page<ReviewResponse>> getReviews(
            @PathVariable Long propertyId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.success("후기 목록 조회 성공", reviewService.getReviews(propertyId, pageable));
    }

    @Operation(summary = "후기 등록")
    @PostMapping
    public ApiResponse<ReviewResponse> createReview(
            @PathVariable Long propertyId,
            @Valid @RequestBody ReviewRequest request
    ) {
        return ApiResponse.success("후기가 등록되었습니다.", reviewService.createReview(propertyId, request));
    }
}
