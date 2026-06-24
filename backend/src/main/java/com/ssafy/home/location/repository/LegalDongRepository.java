package com.ssafy.home.location.repository;

import com.ssafy.home.location.entity.LegalDong;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LegalDongRepository extends JpaRepository<LegalDong, Long> {

    @Query("""
            SELECT legalDong
            FROM LegalDong legalDong
            WHERE legalDong.active = true
              AND legalDong.fullName LIKE CONCAT('%', :keyword, '%')
            ORDER BY
              CASE WHEN legalDong.dong IS NULL THEN 1 ELSE 0 END,
              LENGTH(legalDong.fullName),
              legalDong.fullName
            """)
    List<LegalDong> searchActiveByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
