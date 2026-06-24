package com.ssafy.home.auth.controller;

import com.ssafy.home.auth.dto.request.LoginRequest;
import com.ssafy.home.auth.dto.request.ForgotPasswordRequest;
import com.ssafy.home.auth.dto.request.RefreshTokenRequest;
import com.ssafy.home.auth.dto.request.SignupRequest;
import com.ssafy.home.auth.dto.response.AuthUserResponse;
import com.ssafy.home.auth.dto.response.LoginResponse;
import com.ssafy.home.auth.dto.response.TokenResponse;
import com.ssafy.home.auth.service.AuthService;
import com.ssafy.home.common.response.ApiResponse;
import com.ssafy.home.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "인증 및 권한", description = "회원가입, 로그인, 로그아웃, 토큰 재발급, 내 인증 정보 조회 API")
public class AuthController {

	private final AuthService authService;
	private final CurrentUser currentUser;

	public AuthController(AuthService authService, CurrentUser currentUser) {
		this.authService = authService;
		this.currentUser = currentUser;
	}

	@PostMapping("/signup")
	@Operation(
			summary = "회원가입",
			description = "이메일 기반 회원가입을 처리합니다. 가입 시 BUYER 또는 AGENT 역할을 선택할 수 있으며, AGENT는 전화번호가 필수입니다."
	)
	public ApiResponse<AuthUserResponse> signup(@Valid @RequestBody SignupRequest request) {
		return ApiResponse.success("회원가입에 성공했습니다.", authService.signup(request));
	}

	@PostMapping("/login")
	@Operation(
			summary = "로그인",
			description = "이메일과 비밀번호로 로그인하고 Access Token과 Refresh Token을 발급합니다."
	)
	public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.success("로그인에 성공했습니다.", authService.login(request));
	}

	@PostMapping("/password/forgot")
	@Operation(
			summary = "임시 비밀번호 발급",
			description = "이메일로 임시 비밀번호를 발급합니다. 이메일 존재 여부는 응답에 노출하지 않습니다."
	)
	public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
		authService.issueTemporaryPassword(request);
		return ApiResponse.success("입력한 이메일로 임시 비밀번호 안내를 발송했습니다.", null);
	}

	@PostMapping("/logout")
	@Operation(
			summary = "로그아웃",
			description = "전달받은 Refresh Token을 무효화합니다. 클라이언트는 보유 중인 토큰을 함께 삭제해야 합니다."
	)
	public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
		authService.logout(request);
		return ApiResponse.success("로그아웃에 성공했습니다.", null);
	}

	@PostMapping("/refresh")
	@Operation(
			summary = "토큰 재발급",
			description = "유효한 Refresh Token으로 새로운 Access Token과 Refresh Token을 발급합니다."
	)
	public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return ApiResponse.success("토큰 재발급에 성공했습니다.", authService.refresh(request));
	}

	@GetMapping("/me")
	@Operation(
			summary = "내 인증 정보 조회",
			description = "현재 Access Token에 해당하는 사용자 ID, 이메일, 닉네임, 역할, 계정 상태를 조회합니다."
	)
	public ApiResponse<AuthUserResponse> getMe() {
		return ApiResponse.success("내 인증 정보 조회에 성공했습니다.", authService.getMe(currentUser.getId()));
	}
}
