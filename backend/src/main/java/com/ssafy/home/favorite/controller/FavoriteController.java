package com.ssafy.home.favorite.controller;

import com.ssafy.home.common.annotation.CurrentUser;
import com.ssafy.home.common.response.ApiResponse;
import com.ssafy.home.favorite.dto.FavoriteResponse;
import com.ssafy.home.favorite.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관심 매물", description = "관심 매물 추가, 삭제, 목록 조회 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "관심 매물 추가", description = "매물을 관심 목록에 추가합니다.")
    @PostMapping("/{propertyId}")
    public ResponseEntity<ApiResponse<Void>> addFavorite(
            @PathVariable Long propertyId,
            @Parameter(hidden = true) @CurrentUser Long userId
    ) {
        favoriteService.addFavorite(userId, propertyId);
        return ResponseEntity.ok(ApiResponse.success("관심 매물에 추가되었습니다.", null));
    }

    @Operation(summary = "관심 매물 삭제", description = "관심 목록에서 매물을 삭제합니다.")
    @DeleteMapping("/{propertyId}")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(
            @PathVariable Long propertyId,
            @Parameter(hidden = true) @CurrentUser Long userId
    ) {
        favoriteService.removeFavorite(userId, propertyId);
        return ResponseEntity.ok(ApiResponse.success("관심 매물에서 삭제되었습니다.", null));
    }

    @Operation(summary = "내 관심 매물 목록 조회", description = "현재 로그인한 사용자의 관심 매물 목록을 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<FavoriteResponse>>> getMyFavorites(
            @Parameter(hidden = true) @CurrentUser Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success("관심 매물 목록을 조회했습니다.", favoriteService.getMyFavorites(userId)));
    }
}
