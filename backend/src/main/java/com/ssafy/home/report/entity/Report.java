package com.ssafy.home.report.entity;

import com.ssafy.home.common.entity.BaseTimeEntity;
import com.ssafy.home.property.entity.Property;
import com.ssafy.home.report.type.ReportReason;
import com.ssafy.home.report.type.ReportStatus;
import com.ssafy.home.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "reports",
        uniqueConstraints = @UniqueConstraint(name = "uq_reports", columnNames = {"user_id", "property_id"}),
        indexes = {
                @Index(name = "idx_reports_property", columnList = "property_id"),
                @Index(name = "idx_reports_status", columnList = "status")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportReason reason;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    private LocalDateTime processedAt;

    @Builder
    private Report(User user, Property property, ReportReason reason, String content) {
        this.user = user;
        this.property = property;
        this.reason = reason;
        this.content = content;
        this.status = ReportStatus.PENDING;
    }

    public void updateStatus(ReportStatus status) {
        this.status = status;
        this.processedAt = status == ReportStatus.PENDING ? null : LocalDateTime.now();
    }
}
