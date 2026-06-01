package com.ssafy.home.auth.dto.response;

public record TokenResponse(
		String accessToken,
		String refreshToken,
		String tokenType,
		long expiresIn
) {
}
