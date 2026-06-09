package com.ssafy.home.property.repository;

import com.ssafy.home.property.entity.Property;
import com.ssafy.home.property.entity.PropertyStatus;
import com.ssafy.home.property.entity.RentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PropertyRepository extends JpaRepository<Property, Long>, JpaSpecificationExecutor<Property> {

    Page<Property> findAllByStatusNot(PropertyStatus status, Pageable pageable);

    List<Property> findAllByOwnerIdAndStatusNot(Long ownerId, PropertyStatus status);

    @Query("""
        SELECT p.deposit FROM Property p
        WHERE p.dong = :dong
          AND p.rentType = :rentType
          AND p.area BETWEEN :areaMin AND :areaMax
          AND p.status = :status
          AND p.propertyId != :excludeId
          AND p.deposit IS NOT NULL
        """)
    List<Long> findNearbyDeposits(
            @Param("dong") String dong,
            @Param("rentType") RentType rentType,
            @Param("areaMin") BigDecimal areaMin,
            @Param("areaMax") BigDecimal areaMax,
            @Param("status") PropertyStatus status,
            @Param("excludeId") Long excludeId
    );
}
