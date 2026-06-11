package com.ssafy.home.property.repository;

import com.ssafy.home.property.dto.PropertySearchCondition;
import com.ssafy.home.property.entity.AreaFacilityCount;
import com.ssafy.home.property.entity.Property;
import com.ssafy.home.property.entity.PropertyStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.util.ArrayList;
import java.util.List;

public class PropertySpecification {

    public static Specification<Property> search(PropertySearchCondition condition) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("status"), PropertyStatus.APPROVED));

            if (condition.sido() != null) {
                predicates.add(cb.equal(root.get("sido"), condition.sido()));
            }
            if (condition.gugun() != null) {
                predicates.add(cb.equal(root.get("gugun"), condition.gugun()));
            }
            if (condition.dong() != null) {
                predicates.add(cb.like(root.get("dong"), condition.dong() + "%"));
            }
            if (condition.rentType() != null) {
                predicates.add(cb.equal(root.get("rentType"), condition.rentType()));
            }
            if (condition.roomType() != null) {
                predicates.add(cb.equal(root.get("roomType"), condition.roomType()));
            }
            if (condition.minDeposit() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("deposit"), condition.minDeposit()));
            }
            if (condition.maxDeposit() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("deposit"), condition.maxDeposit()));
            }
            if (condition.minMonthlyRent() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("monthlyRent"), condition.minMonthlyRent()));
            }
            if (condition.maxMonthlyRent() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("monthlyRent"), condition.maxMonthlyRent()));
            }
            if (condition.minArea() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("area"), condition.minArea()));
            }
            if (condition.maxArea() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("area"), condition.maxArea()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Property> inAreas(List<AreaFacilityCount> areas) {
        return (root, query, cb) -> {
            if (areas.isEmpty()) return cb.disjunction();
            List<Predicate> predicates = areas.stream()
                    .map(a -> cb.and(
                            cb.equal(root.get("sido"), a.getSido()),
                            cb.equal(root.get("gugun"), a.getGugun()),
                            cb.equal(root.get("dong"), a.getDong())
                    ))
                    .toList();
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }
}
