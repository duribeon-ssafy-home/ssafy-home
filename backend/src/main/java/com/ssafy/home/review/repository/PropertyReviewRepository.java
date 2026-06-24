package com.ssafy.home.review.repository;

import com.ssafy.home.review.entity.PropertyReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyReviewRepository extends JpaRepository<PropertyReview, Long> {

    Page<PropertyReview> findAllByPropertyId(Long propertyId, Pageable pageable);
}
