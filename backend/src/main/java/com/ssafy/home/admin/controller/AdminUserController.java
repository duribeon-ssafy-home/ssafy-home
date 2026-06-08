package com.ssafy.home.admin.controller;

import com.ssafy.home.admin.service.AdminUserService;
import com.ssafy.home.common.annotation.CurrentUser;
import com.ssafy.home.common.response.ApiResponse;
import com.ssafy.home.user.dto.request.UserRoleUpdateRequest;
import com.ssafy.home.user.dto.request.UserStatusUpdateRequest;
import com.ssafy.home.user.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 회원 관리", description = "관리자 회원 목록, 상세, 상태, 역할 관리 API")
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @Operation(summary = "회원 목록/검색", description = "전체 회원 목록을 조회하거나 keyword로 이메일, 이름, 닉네임을 검색합니다.")
    @GetMapping
    public ApiResponse<List<UserResponse>> getUsers(@RequestParam(required = false) String keyword) {
        return ApiResponse.success("회원 목록을 조회했습니다.", adminUserService.getUsers(keyword));
    }

    @Operation(summary = "회원 상세 조회", description = "회원 ID로 상세 정보를 조회합니다.")
    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getUser(@PathVariable Long userId) {
        return ApiResponse.success("회원 상세 정보를 조회했습니다.", adminUserService.getUser(userId));
    }

    @Operation(summary = "회원 상태 변경", description = "관리자가 회원 계정 상태를 변경합니다.")
    @PatchMapping("/{userId}/status")
    public ApiResponse<UserResponse> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UserStatusUpdateRequest request
    ) {
        return ApiResponse.success("회원 상태를 변경했습니다.", adminUserService.updateUserStatus(userId, request));
    }

    @Operation(summary = "회원 역할 변경", description = "관리자가 회원 역할을 변경합니다.")
    @PatchMapping("/{userId}/role")
    public ApiResponse<UserResponse> updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody UserRoleUpdateRequest request,
            @Parameter(hidden = true) @CurrentUser Long adminUserId
    ) {
        return ApiResponse.success("회원 역할을 변경했습니다.", adminUserService.updateUserRole(userId, adminUserId, request));
    }
}
