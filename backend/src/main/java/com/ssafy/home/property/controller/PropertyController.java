package com.ssafy.home.property.controller;

import com.ssafy.home.common.annotation.CurrentUser;
import com.ssafy.home.common.response.ApiResponse;
import com.ssafy.home.property.dto.PropertyCreateRequest;
import com.ssafy.home.property.dto.PropertyResponse;
import com.ssafy.home.property.dto.PropertySearchCondition;
import com.ssafy.home.property.dto.PropertyUpdateRequest;
import com.ssafy.home.property.service.PropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springdoc.core.annotations.ParameterObject;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "매물", description = "매물 등록, 조회, 수정, 삭제 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    private final PropertyService propertyService;

    @Operation(summary = "매물 목록 조회", description = "삭제되지 않은 전체 매물 목록을 페이지 단위로 반환합니다. 모든 필터는 선택사항입니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PropertyResponse>>> getProperties(
            @ParameterObject PropertySearchCondition condition,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success("매물 목록을 조회했습니다.", propertyService.getProperties(condition, pageable)));
    }

    @Operation(summary = "내 매물 목록 조회", description = "현재 로그인한 사용자가 등록한 매물 목록을 반환합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<PropertyResponse>>> getMyProperties(
            @Parameter(hidden = true) @CurrentUser Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success("내 매물 목록을 조회했습니다.", propertyService.getMyProperties(userId)));
    }

    @Operation(summary = "매물 상세 조회", description = "매물 ID로 단건 상세 정보를 반환합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PropertyResponse>> getProperty(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("매물을 조회했습니다.", propertyService.getProperty(id)));
    }

    @Operation(summary = "매물 등록", description = "새로운 매물을 등록합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createProperty(
            @Valid @RequestBody PropertyCreateRequest request,
            @Parameter(hidden = true) @CurrentUser Long userId
    ) {
        propertyService.createProperty(request, userId);
        return ResponseEntity.ok(ApiResponse.success("매물이 등록되었습니다.", null));
    }

    @Operation(summary = "매물 수정", description = "본인 소유 매물 정보를 수정합니다.")
    @PatchMapping("/{propertyId}")
    public ResponseEntity<ApiResponse<PropertyResponse>> updateProperty(
            @PathVariable Long propertyId,
            @Valid @RequestBody PropertyUpdateRequest request,
            @Parameter(hidden = true) @CurrentUser Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "매물이 수정되었습니다.",
                propertyService.updateProperty(propertyId, request, userId)
        ));
    }

    @Operation(summary = "매물 삭제", description = "본인 소유 매물을 삭제 처리합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProperty(
            @PathVariable Long id,
            @Parameter(hidden = true) @CurrentUser Long userId
    ) {
        propertyService.deleteProperty(id, userId);
        return ResponseEntity.ok(ApiResponse.success("매물이 삭제되었습니다.", null));
    }
}
