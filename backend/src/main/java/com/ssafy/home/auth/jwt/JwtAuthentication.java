package com.ssafy.home.auth.jwt;

import com.ssafy.home.user.type.Role;

public record JwtAuthentication(
		Long userId,
		String email,
		Role role
) {
}
