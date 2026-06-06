package com.ssafy.home.favorite.dto;

import com.ssafy.home.favorite.entity.Favorite;
import com.ssafy.home.property.dto.PropertyResponse;
import java.time.LocalDateTime;

public record FavoriteResponse(
        Long favoriteId,
        PropertyResponse property,
        LocalDateTime createdAt
) {
    public static FavoriteResponse from(Favorite favorite) {
        return new FavoriteResponse(
                favorite.getFavoriteId(),
                PropertyResponse.from(favorite.getProperty()),
                favorite.getCreatedAt()
        );
    }
}
