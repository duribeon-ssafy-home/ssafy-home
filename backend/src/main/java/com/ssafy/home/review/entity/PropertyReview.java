package com.ssafy.home.review.entity;

import com.ssafy.home.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "property_reviews",
        indexes = @Index(name = "idx_property_reviews_property", columnList = "property_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PropertyReview extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder
    private PropertyReview(Long propertyId, String nickname, String content) {
        this.propertyId = propertyId;
        this.nickname = nickname;
        this.content = content;
    }
}
