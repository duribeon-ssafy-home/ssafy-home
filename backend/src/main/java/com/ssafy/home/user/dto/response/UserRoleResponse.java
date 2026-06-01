package com.ssafy.home.user.dto.response;

import com.ssafy.home.user.type.Role;

public record UserRoleResponse(
		Long id,
		Role role,
		String phoneNumber
) {
}
