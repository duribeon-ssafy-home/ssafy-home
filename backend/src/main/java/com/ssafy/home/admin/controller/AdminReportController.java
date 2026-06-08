package com.ssafy.home.admin.controller;

import com.ssafy.home.common.response.ApiResponse;
import com.ssafy.home.report.dto.request.ReportStatusUpdateRequest;
import com.ssafy.home.report.dto.response.ReportResponse;
import com.ssafy.home.report.service.ReportService;
import com.ssafy.home.report.type.ReportStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 신고 관리", description = "관리자 신고 목록, 상세 조회, 처리 상태 변경 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {

    private final ReportService reportService;

    @Operation(summary = "신고 목록 조회", description = "전체 신고 목록을 조회합니다. status를 전달하면 신고 처리 상태별로 필터링합니다.")
    @GetMapping
    public ApiResponse<List<ReportResponse>> getReports(@RequestParam(required = false) ReportStatus status) {
        return ApiResponse.success("신고 목록을 조회했습니다.", reportService.getReports(status));
    }

    @Operation(summary = "신고 상세 조회", description = "신고 ID로 신고 상세 정보를 조회합니다.")
    @GetMapping("/{reportId}")
    public ApiResponse<ReportResponse> getReport(@PathVariable Long reportId) {
        return ApiResponse.success("신고 상세 정보를 조회했습니다.", reportService.getReport(reportId));
    }

    @Operation(summary = "신고 처리 상태 변경", description = "관리자가 신고 처리 상태만 변경합니다. 매물 숨김 처리는 위험 매물/매물 관리 단계에서 별도로 처리합니다.")
    @PatchMapping("/{reportId}")
    public ApiResponse<ReportResponse> updateReportStatus(
            @PathVariable Long reportId,
            @Valid @RequestBody ReportStatusUpdateRequest request
    ) {
        return ApiResponse.success("신고 처리 상태를 변경했습니다.", reportService.updateReportStatus(reportId, request));
    }
}
