package com.ssafy.home.property.service;

import com.ssafy.home.common.exception.BusinessException;
import com.ssafy.home.common.exception.ErrorCode;
import com.ssafy.home.property.dto.PropertyCreateRequest;
import com.ssafy.home.property.dto.PropertyResponse;
import com.ssafy.home.property.dto.PropertySearchCondition;
import com.ssafy.home.property.dto.PropertyUpdateRequest;
import com.ssafy.home.property.entity.AreaFacilityCount;
import com.ssafy.home.property.entity.DataSource;
import com.ssafy.home.property.entity.Property;
import com.ssafy.home.property.entity.PropertyStatus;
import com.ssafy.home.property.repository.AreaFacilityCountRepository;
import com.ssafy.home.property.repository.PropertyRepository;
import com.ssafy.home.property.repository.PropertySpecification;
import com.ssafy.home.user.entity.User;
import com.ssafy.home.user.repository.UserRepository;
import com.ssafy.home.user.type.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class PropertyService {
    private final PropertyRepository propertyRepository;
    private final AreaFacilityCountRepository areaFacilityCountRepository;
    private final UserRepository userRepository;

    @Transactional
    public Long createProperty(PropertyCreateRequest request, Long userId) {
        User user = findUser(userId);
        validateCanCreateProperty(user);

        Property property = Property.builder()
                .ownerId(userId)
                .title(request.title())
                .address(request.address())
                .roadAddress(request.roadAddress())
                .sido(request.sido())
                .gugun(request.gugun())
                .dong(request.dong())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .rentType(request.rentType())
                .roomType(request.roomType())
                .deposit(request.deposit())
                .monthlyRent(request.monthlyRent())
                .area(request.area())
                .floor(request.floor())
                .buildYear(request.buildYear())
                .dealDate(request.dealDate())
                .status(PropertyStatus.APPROVED)
                .dataSource(DataSource.AGENT)
                .build();

        return propertyRepository.save(property).getPropertyId();
    }

    public Page<PropertyResponse> getProperties(PropertySearchCondition condition, Pageable pageable) {
        condition = normalizeCondition(condition);

        Specification<Property> spec = PropertySpecification.search(condition);

        if (condition.facilityCountMin() != null) {
            List<AreaFacilityCount> qualifyingAreas =
                    areaFacilityCountRepository.findWithMinTotalCount(condition.facilityCountMin());
            spec = spec.and(PropertySpecification.inAreas(qualifyingAreas));
        }

        Page<Property> page = propertyRepository.findAll(spec, pageable);

        if (condition.facilityCountMin() != null) {
            Map<String, AreaFacilityCount> facilityMap = page.getContent().stream()
                    .collect(Collectors.toMap(
                            p -> p.getSido() + "|" + p.getGugun() + "|" + p.getDong(),
                            p -> areaFacilityCountRepository
                                    .findBySidoAndGugunAndDong(p.getSido(), p.getGugun(), p.getDong())
                                    .orElse(null),
                            (existing, duplicate) -> existing
                    ));
            return page.map(p -> PropertyResponse.from(p,
                    facilityMap.get(p.getSido() + "|" + p.getGugun() + "|" + p.getDong())));
        }

        return page.map(PropertyResponse::from);
    }

    private PropertySearchCondition normalizeCondition(PropertySearchCondition condition) {
        if (condition == null) {
            return new PropertySearchCondition(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
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
                condition.facilityCountMin()
        );
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public PropertyResponse getProperty(Long id) {
        Property property = findPublicProperty(id);
        AreaFacilityCount area = areaFacilityCountRepository
                .findBySidoAndGugunAndDong(property.getSido(), property.getGugun(), property.getDong())
                .orElse(null);
        return PropertyResponse.from(property, area);
    }

    public PropertyResponse getMyProperty(Long propertyId, Long userId) {
        Property property = findNonDeletedProperty(propertyId);
        if (!property.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.PROPERTY_ACCESS_DENIED);
        }
        return PropertyResponse.from(property);
    }

    public List<PropertyResponse> getMyProperties(Long userId) {
        User user = findUser(userId);
        if (user.getRole() != Role.AGENT) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return propertyRepository.findAllByOwnerIdAndStatusNot(userId, PropertyStatus.DELETED)
                .stream()
                .map(PropertyResponse::from)
                .toList();
    }

    @Transactional
    public PropertyResponse updateProperty(Long id, PropertyUpdateRequest request, Long userId) {
        User user = findUser(userId);
        Property property = findNonDeletedProperty(id);
        validateCanModifyProperty(property, user);
        property.update(request);
        return PropertyResponse.from(property);
    }

    @Transactional
    public void deleteProperty(Long id, Long userId) {
        User user = findUser(userId);
        Property property = findNonDeletedProperty(id);
        validateCanModifyProperty(property, user);
        property.delete();
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private Property findPublicProperty(Long id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROPERTY_NOT_FOUND));
        if (property.getStatus() != PropertyStatus.APPROVED) {
            throw new BusinessException(ErrorCode.PROPERTY_NOT_FOUND);
        }
        return property;
    }

    private Property findNonDeletedProperty(Long id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROPERTY_NOT_FOUND));
        if (property.getStatus() == PropertyStatus.DELETED) {
            throw new BusinessException(ErrorCode.PROPERTY_NOT_FOUND);
        }
        return property;
    }

    private void validateCanCreateProperty(User user) {
        if (user.getRole() != Role.AGENT) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void validateCanModifyProperty(Property property, User user) {
        if (user.getRole() == Role.AGENT && user.getId().equals(property.getOwnerId())) {
            return;
        }
        throw new BusinessException(ErrorCode.PROPERTY_ACCESS_DENIED);
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
}
