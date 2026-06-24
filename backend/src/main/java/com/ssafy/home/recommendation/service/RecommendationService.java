package com.ssafy.home.recommendation.service;

import com.ssafy.home.common.exception.BusinessException;
import com.ssafy.home.common.exception.ErrorCode;
import com.ssafy.home.lifestyle.entity.LifestyleResult;
import com.ssafy.home.lifestyle.repository.LifestyleResultRepository;
import com.ssafy.home.property.dto.PropertyResponse;
import com.ssafy.home.property.dto.PropertySearchCondition;
import com.ssafy.home.property.entity.AreaFacilityCount;
import com.ssafy.home.property.entity.Property;
import com.ssafy.home.property.repository.AreaFacilityCountRepository;
import com.ssafy.home.property.repository.PropertyRepository;
import com.ssafy.home.property.repository.PropertySpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class RecommendationService {

    private final LifestyleResultRepository lifestyleResultRepository;
    private final PropertyRepository propertyRepository;
    private final AreaFacilityCountRepository areaFacilityCountRepository;
    private final RecommendationScorer recommendationScorer;

    public Page<PropertyResponse> recommend(Long userId, PropertySearchCondition condition, Pageable pageable) {
        StopWatch sw = new StopWatch("추천 정렬");

        sw.start("라이프스타일 조회");
        LifestyleResult lifestyle = lifestyleResultRepository
                .findTopByUserIdOrderByCreatedAtDescIdDesc(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIFESTYLE_RESULT_NOT_FOUND));
        sw.stop();

        sw.start("후보 매물 전체 조회");
        Specification<Property> spec = PropertySpecification.search(normalizeCondition(condition));
        List<Property> candidates = propertyRepository.findAll(spec);
        sw.stop();

        sw.start("시설 정보 조회 (N+1 구간)");
        Map<String, AreaFacilityCount> facilityMap = buildFacilityMap(candidates);
        sw.stop();

        sw.start("채점 및 정렬");
        List<ScoredProperty> scoredProperties = candidates.stream()
                .map(property -> {
                    AreaFacilityCount area = facilityMap.get(areaKey(property));
                    int matchScore = recommendationScorer.calculate(lifestyle, property, area);
                    return new ScoredProperty(property, area, matchScore);
                })
                .sorted(scoredPropertyComparator())
                .toList();
        sw.stop();

        sw.start("페이지 변환");
        List<PropertyResponse> content = pageContent(scoredProperties, pageable).stream()
                .map(scored -> PropertyResponse.from(scored.property(), scored.area()))
                .toList();
        sw.stop();

        StringBuilder sb = new StringBuilder();
        for (StopWatch.TaskInfo task : sw.getTaskInfo()) {
            sb.append(String.format("  %-30s %.3fs%n", task.getTaskName(), task.getTimeSeconds()));
        }
        log.info("[추천 정렬] 후보 {}건 | 총 {}s\n{}", candidates.size(),
                String.format("%.3f", sw.getTotalTimeSeconds()), sb);

        return new PageImpl<>(content, pageable, scoredProperties.size());
    }

    private PropertySearchCondition normalizeCondition(PropertySearchCondition condition) {
        if (condition == null) {
            return new PropertySearchCondition(
                    null, null, null, null, null,
                    null, null, null, null, null,
                    null, null, null, null, null, null
            );
        }
        return new PropertySearchCondition(
                normalizeSido(condition.sido()),
                normalizeText(condition.gugun()),
                normalizeText(condition.dong()),
                condition.rentType(),
                condition.roomType(),
                condition.minDeposit(),
                condition.maxDeposit(),
                condition.minMonthlyRent(),
                condition.maxMonthlyRent(),
                condition.minArea(),
                condition.maxArea(),
                null, null, null, null, null
        );
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Comparator<ScoredProperty> scoredPropertyComparator() {
        return Comparator.comparingInt(ScoredProperty::matchScore)
                .reversed()
                .thenComparing(
                        scored -> createdAtOrMin(scored.property()),
                        Comparator.reverseOrder()
                );
    }

    private LocalDateTime createdAtOrMin(Property property) {
        return property.getCreatedAt() != null ? property.getCreatedAt() : LocalDateTime.MIN;
    }

    private List<ScoredProperty> pageContent(List<ScoredProperty> scoredProperties, Pageable pageable) {
        int start = (int) Math.min(pageable.getOffset(), scoredProperties.size());
        int end = Math.min(start + pageable.getPageSize(), scoredProperties.size());
        if (start >= end) {
            return Collections.emptyList();
        }
        return scoredProperties.subList(start, end);
    }

    private Map<String, AreaFacilityCount> buildFacilityMap(List<Property> properties) {
        Map<String, AreaFacilityCount> facilityMap = new HashMap<>();
        Set<String> searchedKeys = new HashSet<>();

        for (Property property : properties) {
            String key = areaKey(property);
            if (!searchedKeys.add(key)) {
                continue;
            }
            areaFacilityCountRepository
                    .findBySidoAndGugunAndDong(property.getSido(), property.getGugun(), property.getDong())
                    .ifPresent(area -> facilityMap.put(key, area));
        }

        return facilityMap;
    }

    private String areaKey(Property property) {
        return property.getSido() + "|" + property.getGugun() + "|" + property.getDong();
    }

    private String normalizeSido(String sido) {
        String value = normalizeText(sido);
        if (value == null) return null;
        return switch (value) {
            case "서울", "서울시" -> "서울특별시";
            case "부산", "부산시" -> "부산광역시";
            case "대구", "대구시" -> "대구광역시";
            case "인천", "인천시" -> "인천광역시";
            case "광주", "광주시" -> "광주광역시";
            case "대전", "대전시" -> "대전광역시";
            case "울산", "울산시" -> "울산광역시";
            case "세종", "세종시" -> "세종특별자치시";
            case "경기" -> "경기도";
            case "강원", "강원도" -> "강원특별자치도";
            case "충북" -> "충청북도";
            case "충남" -> "충청남도";
            case "전남" -> "전라남도";
            case "전북", "전북도" -> "전북특별자치도";
            case "경남" -> "경상남도";
            case "경북" -> "경상북도";
            case "제주", "제주도" -> "제주특별자치도";
            default -> value;
        };
    }

    private record ScoredProperty(Property property, AreaFacilityCount area, int matchScore) {
    }
}
