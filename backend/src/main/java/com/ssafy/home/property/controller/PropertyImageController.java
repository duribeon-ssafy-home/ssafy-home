package com.ssafy.home.property.controller;

import com.ssafy.home.common.annotation.CurrentUser;
import com.ssafy.home.common.response.ApiResponse;
import com.ssafy.home.property.dto.PropertyImageResponse;
import com.ssafy.home.property.service.PropertyImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.ssafy.home.property.dto.PropertyImageUpdateRequest;
import jakarta.validation.Valid;

import java.util.List;

@Tag(name = "매물 이미지", description = "매물 이미지 업로드, 삭제 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/properties/{propertyId}/images")
public class PropertyImageController {

    private final PropertyImageService propertyImageService;

    @Operation(summary = "이미지 업로드", description = "매물에 이미지를 업로드합니다. 최대 10장까지 등록 가능하며, 공공 데이터 매물은 불가합니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<PropertyImageResponse>>> uploadImages(
            @PathVariable Long propertyId,
            @RequestPart List<MultipartFile> files,
            @Parameter(hidden = true) @CurrentUser Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "이미지가 업로드되었습니다.",
                propertyImageService.uploadImages(propertyId, userId, files)
        ));
    }

    @Operation(summary = "이미지 순서 변경", description = "이미지의 표시 순서를 변경합니다. sortOrder가 0인 이미지가 대표 이미지로 표시됩니다.")
    @PatchMapping("/{imageId}")
    public ResponseEntity<ApiResponse<PropertyImageResponse>> updateSortOrder(
            @PathVariable Long propertyId,
            @PathVariable Long imageId,
            @Valid @RequestBody PropertyImageUpdateRequest request,
            @Parameter(hidden = true) @CurrentUser Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "이미지 순서가 변경되었습니다.",
                propertyImageService.updateSortOrder(propertyId, imageId, userId, request)
        ));
    }

    @Operation(summary = "이미지 삭제", description = "매물에 등록된 이미지를 삭제합니다.")
    @DeleteMapping("/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable Long propertyId,
            @PathVariable Long imageId,
            @Parameter(hidden = true) @CurrentUser Long userId
    ) {
        propertyImageService.deleteImage(propertyId, imageId, userId);
        return ResponseEntity.ok(ApiResponse.success("이미지가 삭제되었습니다.", null));
    }
}
