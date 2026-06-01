package com.ssafy.home.user.dto.request;

import com.ssafy.home.user.type.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UserStatusUpdateRequest(
		@NotNull UserStatus status
) {
}
