package com.ssafy.home.user.dto.request;

import com.ssafy.home.user.type.Role;
import jakarta.validation.constraints.NotNull;

public record UserRoleUpdateRequest(
		@NotNull Role role,
		String phoneNumber
) {
}
