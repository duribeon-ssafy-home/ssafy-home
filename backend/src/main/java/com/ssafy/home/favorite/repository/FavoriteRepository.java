package com.ssafy.home.favorite.repository;

import com.ssafy.home.favorite.entity.Favorite;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByUserIdAndPropertyPropertyId(Long userId, Long propertyId);

    Optional<Favorite> findByUserIdAndPropertyPropertyId(Long userId, Long propertyId);

    List<Favorite> findAllByUserId(Long userId);
}
