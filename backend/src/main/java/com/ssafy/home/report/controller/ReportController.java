package com.ssafy.home.report.controller;

import com.ssafy.home.common.annotation.CurrentUser;
import com.ssafy.home.common.response.ApiResponse;
import com.ssafy.home.report.dto.request.ReportCreateRequest;
import com.ssafy.home.report.dto.response.ReportResponse;
import com.ssafy.home.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "신고", description = "매물 신고 접수 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/properties/{propertyId}/reports")
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "매물 신고 접수", description = "로그인한 사용자가 특정 매물에 대한 신고를 접수합니다.")
    @PostMapping
    public ApiResponse<ReportResponse> createReport(
            @PathVariable Long propertyId,
            @Valid @RequestBody ReportCreateRequest request,
            @Parameter(hidden = true) @CurrentUser Long userId
    ) {
        return ApiResponse.success("신고가 접수되었습니다.", reportService.createReport(propertyId, userId, request));
    }
}
