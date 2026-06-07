package com.ssafy.home.property.repository;

import com.ssafy.home.property.entity.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PropertyImageRepository extends JpaRepository<PropertyImage, Long>{
    List<PropertyImage> findByProperty_PropertyId(Long propertyId);

    int countByProperty_PropertyId(Long propertyId);

    @Query("SELECT COALESCE(MAX(i.sortOrder), -1) FROM PropertyImage i WHERE i.property.propertyId = :propertyId")
    int findMaxSortOrderByPropertyId(@Param("propertyId") Long propertyId);

    java.util.Optional<PropertyImage> findByProperty_PropertyIdAndSortOrder(Long propertyId, Integer sortOrder);
}
