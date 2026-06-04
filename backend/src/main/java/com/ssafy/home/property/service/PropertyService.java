package com.ssafy.home.property.service;

import com.ssafy.home.common.exception.BusinessException;
import com.ssafy.home.common.exception.ErrorCode;
import com.ssafy.home.property.dto.PropertyCreateRequest;
import com.ssafy.home.property.dto.PropertyResponse;
import com.ssafy.home.property.dto.PropertyUpdateRequest;
import com.ssafy.home.property.entity.DataSource;
import com.ssafy.home.property.entity.Property;
import com.ssafy.home.property.entity.PropertyStatus;
import com.ssafy.home.property.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class PropertyService {
    private final PropertyRepository propertyRepository;

    @Transactional
    public void createProperty(PropertyCreateRequest request, Long userId) {
        Property property = Property.builder()
                .ownerId(userId)
                .address(request.address())
                .sido(request.sido())
                .gugun(request.gugun())
                .dong(request.dong())
                .status(PropertyStatus.PENDING)
                .dataSource(DataSource.AGENT)
                .build();

        propertyRepository.save(property);
    }

    public List<PropertyResponse> getProperties() {
        return propertyRepository.findAllByStatusNot(PropertyStatus.DELETED)
                .stream()
                .map(PropertyResponse::from)
                .toList();
    }

    public PropertyResponse getProperty(Long id) {
        Property property = findActiveProperty(id);
        return PropertyResponse.from(property);
    }

    @Transactional
    public PropertyResponse updateProperty(Long id, PropertyUpdateRequest request, Long userId) {
        Property property = findActiveProperty(id);
        validateOwner(property, userId);
        property.update(request);
        return PropertyResponse.from(property);
    }

    @Transactional
    public void deleteProperty(Long id, Long userId) {
        Property property = findActiveProperty(id);
        validateOwner(property, userId);
        property.delete();
    }

    private Property findActiveProperty(Long id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROPERTY_NOT_FOUND));
        if (property.getStatus() == PropertyStatus.DELETED) {
            throw new BusinessException(ErrorCode.PROPERTY_NOT_FOUND);
        }
        return property;
    }

    private void validateOwner(Property property, Long userId) {
        if (!property.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.PROPERTY_ACCESS_DENIED);
        }
    }
}
