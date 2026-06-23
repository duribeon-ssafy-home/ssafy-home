package com.ssafy.home.favorite.dto;

import com.ssafy.home.property.dto.PropertyImageResponse;
import com.ssafy.home.property.entity.DataSource;
import com.ssafy.home.property.entity.Property;
import com.ssafy.home.property.entity.PropertyImage;
import com.ssafy.home.property.entity.PropertyStatus;
import com.ssafy.home.property.entity.RentType;
import com.ssafy.home.property.entity.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record FavoritePropertySummaryResponse(
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
        List<PropertyImageResponse> images,
        List<String> tags
) {
    public static FavoritePropertySummaryResponse from(Property property, PropertyImage representativeImage) {
        return new FavoritePropertySummaryResponse(
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
                representativeImage == null ? List.of() : List.of(PropertyImageResponse.from(representativeImage)),
                List.of()
        );
    }
}
