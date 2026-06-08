package com.ssafy.home.report.dto.response;

import com.ssafy.home.property.entity.Property;
import com.ssafy.home.report.entity.Report;
import com.ssafy.home.report.type.ReportReason;
import com.ssafy.home.report.type.ReportStatus;
import com.ssafy.home.user.entity.User;
import java.time.LocalDateTime;

public record ReportResponse(
        Long reportId,
        Long userId,
        String reporterEmail,
        String reporterNickname,
        Long propertyId,
        String propertyTitle,
        String propertyAddress,
        ReportReason reason,
        String content,
        ReportStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime processedAt
) {

    public static ReportResponse from(Report report) {
        User user = report.getUser();
        Property property = report.getProperty();

        return new ReportResponse(
                report.getId(),
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                property.getPropertyId(),
                property.getTitle(),
                property.getAddress(),
                report.getReason(),
                report.getContent(),
                report.getStatus(),
                report.getCreatedAt(),
                report.getUpdatedAt(),
                report.getProcessedAt()
        );
    }
}
