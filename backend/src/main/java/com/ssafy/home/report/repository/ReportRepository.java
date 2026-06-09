package com.ssafy.home.report.repository;

import com.ssafy.home.report.entity.Report;
import com.ssafy.home.report.type.ReportStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByUser_IdAndProperty_PropertyId(Long userId, Long propertyId);

    List<Report> findAllByOrderByCreatedAtDescIdDesc();

    List<Report> findAllByStatusOrderByCreatedAtDescIdDesc(ReportStatus status);

    List<Report> findAllByProperty_PropertyId(Long propertyId);
}
