package com.ssafy.home.lifestyle.repository;

import com.ssafy.home.lifestyle.entity.LifestyleResult;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LifestyleResultRepository extends JpaRepository<LifestyleResult, Long> {

    Optional<LifestyleResult> findTopByUserIdOrderByCreatedAtDescIdDesc(Long userId);
}
