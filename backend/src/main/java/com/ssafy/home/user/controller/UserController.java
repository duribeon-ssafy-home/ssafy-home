package com.ssafy.home.user.controller;

import com.ssafy.home.common.response.ApiResponse;
import com.ssafy.home.common.security.CurrentUser;
import com.ssafy.home.user.dto.request.PasswordChangeRequest;
import com.ssafy.home.user.dto.request.UserRoleUpdateRequest;
import com.ssafy.home.user.dto.request.UserStatusUpdateRequest;
import com.ssafy.home.user.dto.request.UserUpdateRequest;
import com.ssafy.home.user.dto.response.UserResponse;
import com.ssafy.home.user.dto.response.UserRoleResponse;
import com.ssafy.home.user.dto.response.UserStatusResponse;
import com.ssafy.home.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "사용자 정보", description = "내 사용자 정보 조회, 수정, 계정 상태 변경, 역할 변경 API")
public class UserController {

	private final UserService userService;
	private final CurrentUser currentUser;

	public UserController(UserService userService, CurrentUser currentUser) {
		this.userService = userService;
		this.currentUser = currentUser;
	}

	@GetMapping("/me")
	@Operation(
			summary = "내 정보 조회",
			description = "현재 로그인한 사용자의 상세 프로필 정보를 조회합니다."
	)
	public ApiResponse<UserResponse> getMe() {
		return ApiResponse.success("내 정보 조회에 성공했습니다.", userService.getMe(currentUser.getId()));
	}

	@PatchMapping("/me")
	@Operation(
			summary = "내 정보 수정",
			description = "현재 로그인한 사용자의 이름, 닉네임, 전화번호를 수정합니다."
	)
	public ApiResponse<UserResponse> updateMe(@Valid @RequestBody UserUpdateRequest request) {
		return ApiResponse.success("내 정보 수정에 성공했습니다.", userService.updateMe(currentUser.getId(), request));
	}

	@PatchMapping("/me/password")
	@Operation(
			summary = "내 비밀번호 변경",
			description = "현재 비밀번호를 확인한 뒤 새 비밀번호로 변경합니다."
	)
	public ApiResponse<Void> changeMyPassword(@Valid @RequestBody PasswordChangeRequest request) {
		userService.changeMyPassword(currentUser.getId(), request);
		return ApiResponse.success("비밀번호가 변경되었습니다.", null);
	}

	@PatchMapping("/me/status")
	@Operation(
			summary = "내 계정 상태 변경",
			description = "현재 로그인한 BUYER 또는 AGENT 계정을 비활성화하거나 탈퇴 상태로 변경합니다."
	)	public ApiResponse<UserStatusResponse> updateMyStatus(@Valid @RequestBody UserStatusUpdateRequest request) {
		return ApiResponse.success("내 계정 상태 변경에 성공했습니다.", userService.updateMyStatus(currentUser.getId(), request));
	}

	@PatchMapping("/me/role")
	@Operation(
			summary = "내 역할 변경",
			description = "현재 로그인한 BUYER 사용자를 AGENT로 변경합니다. AGENT 전환 시 전화번호가 필수입니다."
	)
	public ApiResponse<UserRoleResponse> updateMyRole(@Valid @RequestBody UserRoleUpdateRequest request) {
		return ApiResponse.success("내 역할 변경에 성공했습니다.", userService.updateMyRole(currentUser.getId(), request));
	}
}
