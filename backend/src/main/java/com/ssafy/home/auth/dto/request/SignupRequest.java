package com.ssafy.home.auth.dto.request;

import com.ssafy.home.user.type.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SignupRequest(
		@NotBlank @Email String email,
		@NotBlank String password,
		@NotBlank String name,
		@NotBlank String nickname,
		String phoneNumber,
		@NotNull Role role
) {
}
