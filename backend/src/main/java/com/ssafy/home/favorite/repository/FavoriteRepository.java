package com.ssafy.home.favorite.repository;

import com.ssafy.home.favorite.entity.Favorite;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByUserIdAndPropertyPropertyId(Long userId, Long propertyId);

    Optional<Favorite> findByUserIdAndPropertyPropertyId(Long userId, Long propertyId);

    List<Favorite> findAllByUserId(Long userId);

    @Query("""
            select f
            from Favorite f
            join fetch f.property
            where f.userId = :userId
            order by f.createdAt desc
            """)
    List<Favorite> findAllByUserIdWithProperty(@Param("userId") Long userId);
}
