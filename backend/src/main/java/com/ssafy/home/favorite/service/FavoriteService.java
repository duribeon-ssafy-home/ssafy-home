package com.ssafy.home.favorite.service;

import com.ssafy.home.common.exception.BusinessException;
import com.ssafy.home.common.exception.ErrorCode;
import com.ssafy.home.favorite.dto.FavoriteResponse;
import com.ssafy.home.favorite.entity.Favorite;
import com.ssafy.home.favorite.repository.FavoriteRepository;
import com.ssafy.home.property.entity.Property;
import com.ssafy.home.property.entity.PropertyStatus;
import com.ssafy.home.property.repository.PropertyRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final PropertyRepository propertyRepository;

    @Transactional
    public void addFavorite(Long userId, Long propertyId) {
        if (favoriteRepository.existsByUserIdAndPropertyPropertyId(userId, propertyId)) {
            throw new BusinessException(ErrorCode.FAVORITE_ALREADY_EXISTS);
        }
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROPERTY_NOT_FOUND));
        if (property.getStatus() == PropertyStatus.DELETED) {
            throw new BusinessException(ErrorCode.PROPERTY_NOT_FOUND);
        }

        favoriteRepository.save(Favorite.builder()
                .userId(userId)
                .property(property)
                .build());
    }

    @Transactional
    public void removeFavorite(Long userId, Long propertyId) {
        Favorite favorite = favoriteRepository.findByUserIdAndPropertyPropertyId(userId, propertyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FAVORITE_NOT_FOUND));
        favoriteRepository.delete(favorite);
    }

    public List<FavoriteResponse> getMyFavorites(Long userId) {
        return favoriteRepository.findAllByUserId(userId)
                .stream()
                .map(FavoriteResponse::from)
                .toList();
    }
}
