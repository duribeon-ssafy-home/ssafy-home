package com.ssafy.home.property.repository;

import com.ssafy.home.property.entity.AreaFacilityCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AreaFacilityCountRepository extends JpaRepository<AreaFacilityCount, Long> {

    Optional<AreaFacilityCount> findBySidoAndGugunAndDong(String sido, String gugun, String dong);

    @Query("""
            SELECT a FROM AreaFacilityCount a
            WHERE (COALESCE(a.subwayCount500m, 0)
                 + COALESCE(a.martCount1km, 0)
                 + COALESCE(a.convenienceCount500m, 0)
                 + COALESCE(a.hospitalCount1km, 0)
                 + COALESCE(a.pharmacyCount500m, 0)
                 + COALESCE(a.cafeCount500m, 0)
                 + COALESCE(a.restaurantCount500m, 0)) >= :minCount
            """)
    List<AreaFacilityCount> findWithMinTotalCount(@Param("minCount") int minCount);
}
