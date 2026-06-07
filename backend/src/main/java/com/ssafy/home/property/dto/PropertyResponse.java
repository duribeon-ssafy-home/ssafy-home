package com.ssafy.home.property.dto;

import com.ssafy.home.property.entity.DataSource;
import com.ssafy.home.property.entity.Property;
import com.ssafy.home.property.entity.PropertyStatus;
import com.ssafy.home.property.entity.RentType;
import com.ssafy.home.property.entity.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PropertyResponse(
        Long propertyId,
        Long ownerId,
        String title,
        String address,
        String roadAddress,
        String sido,
        String gugun,
        String dong,
        BigDecimal latitude,
        BigDecimal longitude,
        RentType rentType,
        RoomType roomType,
        Long deposit,
        Integer monthlyRent,
        BigDecimal area,
        Integer floor,
        Integer buildYear,
        DataSource dataSource,
        LocalDate dealDate,
        PropertyStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<PropertyImageResponse> images
) {
    public static PropertyResponse from(Property property) {
        return new PropertyResponse(
                property.getPropertyId(),
                property.getOwnerId(),
                property.getTitle(),
                property.getAddress(),
                property.getRoadAddress(),
                property.getSido(),
                property.getGugun(),
                property.getDong(),
                property.getLatitude(),
                property.getLongitude(),
                property.getRentType(),
                property.getRoomType(),
                property.getDeposit(),
                property.getMonthlyRent(),
                property.getArea(),
                property.getFloor(),
                property.getBuildYear(),
                property.getDataSource(),
                property.getDealDate(),
                property.getStatus(),
                property.getCreatedAt(),
                property.getUpdatedAt(),
                property.getImages().stream().map(PropertyImageResponse::from).toList()
        );
    }
}
