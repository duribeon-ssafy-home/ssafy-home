package com.ssafy.home.report.dto.request;

import com.ssafy.home.report.type.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportCreateRequest(
        @NotNull ReportReason reason,

        @Size(max = 2000)
        String content
) {
}
