package com.ssafy.home.report.dto.request;

import com.ssafy.home.report.type.ReportStatus;
import jakarta.validation.constraints.NotNull;

public record ReportStatusUpdateRequest(
        @NotNull ReportStatus status
) {
}
