package com.ssafy.home.location.controller;

import com.ssafy.home.common.response.ApiResponse;
import com.ssafy.home.location.dto.response.LocationSearchResponse;
import com.ssafy.home.location.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "지역", description = "법정동 기반 지역 검색 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationService locationService;

    @Operation(
            summary = "지역 자동완성 후보 조회",
            description = "법정동명 일부를 입력하면 활성 법정동 후보를 최대 10개 반환합니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<LocationSearchResponse>>> search(
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(ApiResponse.success("지역 검색 후보를 조회했습니다.", locationService.search(keyword)));
    }
}
