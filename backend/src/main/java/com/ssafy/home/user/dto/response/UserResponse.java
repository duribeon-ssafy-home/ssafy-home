package com.ssafy.home.user.dto.response;

import com.ssafy.home.user.entity.User;
import com.ssafy.home.user.type.AuthProvider;
import com.ssafy.home.user.type.Role;
import com.ssafy.home.user.type.UserStatus;
import java.time.LocalDateTime;

public record UserResponse(
		Long id,
		String email,
		String name,
		String nickname,
		String phoneNumber,
		Role role,
		UserStatus status,
		AuthProvider provider,
		LocalDateTime lastLoginAt,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {

	public static UserResponse from(User user) {
		return new UserResponse(
				user.getId(),
				user.getEmail(),
				user.getName(),
				user.getNickname(),
				user.getPhoneNumber(),
				user.getRole(),
				user.getStatus(),
				user.getProvider(),
				user.getLastLoginAt(),
				user.getCreatedAt(),
				user.getUpdatedAt()
		);
	}
}
