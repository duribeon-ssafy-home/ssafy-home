package com.ssafy.home.auth.dto.response;

import com.ssafy.home.user.entity.User;
import com.ssafy.home.user.type.AuthProvider;
import com.ssafy.home.user.type.Role;
import com.ssafy.home.user.type.UserStatus;

public record AuthUserResponse(
		Long id,
		String email,
		String nickname,
		Role role,
		UserStatus status,
		AuthProvider provider
) {

	public static AuthUserResponse from(User user) {
		return new AuthUserResponse(
				user.getId(),
				user.getEmail(),
				user.getNickname(),
				user.getRole(),
				user.getStatus(),
				user.getProvider()
		);
	}
}
