package com.ssafy.home.property.repository;

import com.ssafy.home.property.entity.Property;
import com.ssafy.home.property.entity.PropertyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface PropertyRepository extends JpaRepository<Property, Long>, JpaSpecificationExecutor<Property> {

    Page<Property> findAllByStatusNot(PropertyStatus status, Pageable pageable);

    List<Property> findAllByOwnerIdAndStatusNot(Long ownerId, PropertyStatus status);
}
