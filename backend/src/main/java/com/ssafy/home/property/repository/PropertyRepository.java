package com.ssafy.home.property.repository;

import com.ssafy.home.property.entity.Property;
import com.ssafy.home.property.entity.PropertyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    List<Property> findAllByStatusNot(PropertyStatus status);

    List<Property> findAllByOwnerIdAndStatusNot(Long ownerId, PropertyStatus status);
}
