package com.ssafy.home.recommendation.service;

import com.ssafy.home.common.exception.BusinessException;
import com.ssafy.home.common.exception.ErrorCode;
import com.ssafy.home.lifestyle.entity.LifestyleResult;
import com.ssafy.home.lifestyle.repository.LifestyleResultRepository;
import com.ssafy.home.property.dto.FacilityInfo;
import com.ssafy.home.property.dto.PropertyResponse;
import com.ssafy.home.property.entity.AreaFacilityCount;
import com.ssafy.home.property.entity.Property;
import com.ssafy.home.property.entity.PropertyStatus;
import com.ssafy.home.property.repository.AreaFacilityCountRepository;
import com.ssafy.home.property.repository.PropertyRepository;
import com.ssafy.home.property.repository.PropertySpecification;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class RecommendationService {

    private final LifestyleResultRepository lifestyleResultRepository;
    private final PropertyRepository propertyRepository;
    private final AreaFacilityCountRepository areaFacilityCountRepository;

    public Page<PropertyResponse> recommend(Long userId, Pageable pageable) {
        LifestyleResult lifestyle = lifestyleResultRepository
                .findTopByUserIdOrderByCreatedAtDescIdDesc(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIFESTYLE_RESULT_NOT_FOUND));

        Specification<Property> spec = buildSpec(lifestyle);

        if (lifestyle.getFacilityCountMin() != null) {
            List<AreaFacilityCount> qualifyingAreas =
                    areaFacilityCountRepository.findWithMinTotalCount(lifestyle.getFacilityCountMin());
            spec = spec.and(PropertySpecification.inAreas(qualifyingAreas));
        }

        Page<Property> page = propertyRepository.findAll(spec, pageable);

        Map<String, AreaFacilityCount> facilityMap = buildFacilityMap(page.getContent());

        return page.map(p -> PropertyResponse.from(p,
                facilityMap.get(areaKey(p.getSido(), p.getGugun(), p.getDong()))));
    }

    private Specification<Property> buildSpec(LifestyleResult lifestyle) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("status"), PropertyStatus.APPROVED));

            if (lifestyle.getMonthlyRentMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("monthlyRent"), lifestyle.getMonthlyRentMax()));
            }
            if (lifestyle.getDepositMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("deposit"), lifestyle.getDepositMax()));
            }
            if (lifestyle.getAreaMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("area"), lifestyle.getAreaMin()));
            }
            if (lifestyle.getBuildYearMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("buildYear"), lifestyle.getBuildYearMin()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Map<String, AreaFacilityCount> buildFacilityMap(List<Property> properties) {
        return properties.stream()
                .collect(Collectors.toMap(
                        p -> areaKey(p.getSido(), p.getGugun(), p.getDong()),
                        p -> areaFacilityCountRepository
                                .findBySidoAndGugunAndDong(p.getSido(), p.getGugun(), p.getDong())
                                .orElse(null),
                        (existing, duplicate) -> existing
                ))
                .entrySet().stream()
                .filter(e -> e.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private String areaKey(String sido, String gugun, String dong) {
        return sido + "|" + gugun + "|" + dong;
    }
}
