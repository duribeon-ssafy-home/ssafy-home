package com.ssafy.home.property.dto;

import com.ssafy.home.property.entity.PropertyImage;

public record PropertyImageResponse(
        Long imageId,
        String imageUrl,
        Integer sortOrder
) {
    public static PropertyImageResponse from(PropertyImage image){
        return new PropertyImageResponse(
                image.getImageId(),
                image.getImageUrl(),
                image.getSortOrder()
        );
    }
}
