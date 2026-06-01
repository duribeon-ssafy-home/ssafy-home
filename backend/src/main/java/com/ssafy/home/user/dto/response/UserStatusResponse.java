package com.ssafy.home.user.dto.response;

import com.ssafy.home.user.type.UserStatus;

public record UserStatusResponse(
		Long id,
		UserStatus status
) {
}
