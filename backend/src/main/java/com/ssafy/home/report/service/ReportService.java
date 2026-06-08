package com.ssafy.home.report.service;

import com.ssafy.home.common.exception.BusinessException;
import com.ssafy.home.common.exception.ErrorCode;
import com.ssafy.home.property.entity.Property;
import com.ssafy.home.property.entity.PropertyStatus;
import com.ssafy.home.property.repository.PropertyRepository;
import com.ssafy.home.report.dto.request.ReportCreateRequest;
import com.ssafy.home.report.dto.request.ReportStatusUpdateRequest;
import com.ssafy.home.report.dto.response.ReportResponse;
import com.ssafy.home.report.entity.Report;
import com.ssafy.home.report.repository.ReportRepository;
import com.ssafy.home.report.type.ReportStatus;
import com.ssafy.home.user.entity.User;
import com.ssafy.home.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;

    @Transactional
    public ReportResponse createReport(Long propertyId, Long userId, ReportCreateRequest request) {
        User user = findUser(userId);
        Property property = findReportableProperty(propertyId);

        if (reportRepository.existsByUser_IdAndProperty_PropertyId(userId, propertyId)) {
            throw new BusinessException(ErrorCode.REPORT_ALREADY_EXISTS);
        }

        Report report = Report.builder()
                .user(user)
                .property(property)
                .reason(request.reason())
                .content(normalizeContent(request.content()))
                .build();

        return ReportResponse.from(reportRepository.save(report));
    }

    public List<ReportResponse> getReports(ReportStatus status) {
        List<Report> reports = status == null
                ? reportRepository.findAllByOrderByCreatedAtDescIdDesc()
                : reportRepository.findAllByStatusOrderByCreatedAtDescIdDesc(status);

        return reports.stream()
                .map(ReportResponse::from)
                .toList();
    }

    public ReportResponse getReport(Long reportId) {
        return ReportResponse.from(findReport(reportId));
    }

    @Transactional
    public ReportResponse updateReportStatus(Long reportId, ReportStatusUpdateRequest request) {
        Report report = findReport(reportId);

        // 현재 단계의 HIDDEN은 신고 검토 결과만 의미합니다.
        // 실제 Property.status 변경과 위험 라벨 처리는 이후 관리자 위험 매물/매물 관리 흐름에서 연결합니다.
        report.updateStatus(request.status());

        return ReportResponse.from(report);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private Property findReportableProperty(Long propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROPERTY_NOT_FOUND));

        if (property.getStatus() == PropertyStatus.DELETED) {
            throw new BusinessException(ErrorCode.PROPERTY_NOT_FOUND);
        }

        return property;
    }

    private Report findReport(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));
    }

    private String normalizeContent(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        return content.trim();
    }
}
