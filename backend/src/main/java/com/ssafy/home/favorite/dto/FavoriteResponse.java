package com.ssafy.home.favorite.dto;

import com.ssafy.home.favorite.entity.Favorite;
import com.ssafy.home.property.entity.PropertyImage;
import java.time.LocalDateTime;

public record FavoriteResponse(
        Long favoriteId,
        FavoritePropertySummaryResponse property,
        LocalDateTime createdAt
) {
    public static FavoriteResponse from(Favorite favorite, PropertyImage representativeImage) {
        return new FavoriteResponse(
                favorite.getFavoriteId(),
                FavoritePropertySummaryResponse.from(favorite.getProperty(), representativeImage),
                favorite.getCreatedAt()
        );
    }
}
