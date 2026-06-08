package com.ssafy.home.property.service;

import com.ssafy.home.common.exception.BusinessException;
import com.ssafy.home.common.exception.ErrorCode;
import com.ssafy.home.property.dto.PropertyCreateRequest;
import com.ssafy.home.property.dto.PropertyResponse;
import com.ssafy.home.property.dto.PropertySearchCondition;
import com.ssafy.home.property.dto.PropertyUpdateRequest;
import com.ssafy.home.property.repository.PropertySpecification;
import com.ssafy.home.property.entity.DataSource;
import com.ssafy.home.property.entity.Property;
import com.ssafy.home.property.entity.PropertyStatus;
import com.ssafy.home.property.repository.PropertyRepository;
import com.ssafy.home.user.entity.User;
import com.ssafy.home.user.repository.UserRepository;
import com.ssafy.home.user.type.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class PropertyService {
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createProperty(PropertyCreateRequest request, Long userId) {
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
                .status(PropertyStatus.PENDING)
                .dataSource(DataSource.AGENT)
                .build();

        propertyRepository.save(property);
    }

    public Page<PropertyResponse> getProperties(PropertySearchCondition condition, Pageable pageable) {
        return propertyRepository.findAll(PropertySpecification.search(condition), pageable)
                .map(PropertyResponse::from);
    }

    public PropertyResponse getProperty(Long id) {
        Property property = findPublicProperty(id);
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
}
