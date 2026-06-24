package com.ssafy.home.user.service;

import com.ssafy.home.common.exception.BusinessException;
import com.ssafy.home.common.exception.ErrorCode;
import com.ssafy.home.user.dto.request.PasswordChangeRequest;
import com.ssafy.home.user.dto.request.UserRoleUpdateRequest;
import com.ssafy.home.user.dto.request.UserStatusUpdateRequest;
import com.ssafy.home.user.dto.request.UserUpdateRequest;
import com.ssafy.home.user.dto.response.UserResponse;
import com.ssafy.home.user.dto.response.UserRoleResponse;
import com.ssafy.home.user.dto.response.UserStatusResponse;
import com.ssafy.home.user.entity.User;
import com.ssafy.home.user.repository.UserRepository;
import com.ssafy.home.user.type.Role;
import com.ssafy.home.user.type.UserStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public UserResponse getMe(Long userId) {
		return UserResponse.from(findUser(userId));
	}

	@Transactional
	public UserResponse updateMe(Long userId, UserUpdateRequest request) {
		User user = findUser(userId);
		user.updateProfile(request.name(), request.nickname(), request.phoneNumber());
		return UserResponse.from(user);
	}

	@Transactional
	public void changeMyPassword(Long userId, PasswordChangeRequest request) {
		User user = findUser(userId);
		if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
			throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
		}

		user.changePassword(passwordEncoder.encode(request.newPassword()));
	}

	@Transactional
	public UserStatusResponse updateMyStatus(Long userId, UserStatusUpdateRequest request) {
		User user = findUser(userId);
		if (user.getRole() == Role.ADMIN) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		if (request.status() != UserStatus.INACTIVE && request.status() != UserStatus.DELETED) {
			throw new BusinessException(ErrorCode.INVALID_STATUS);
		}

		user.changeStatus(request.status());
		return new UserStatusResponse(user.getId(), user.getStatus());
	}

	@Transactional
	public UserRoleResponse updateMyRole(Long userId, UserRoleUpdateRequest request) {
		User user = findUser(userId);
		if (user.getRole() != Role.BUYER || request.role() != Role.AGENT) {
			throw new BusinessException(ErrorCode.INVALID_ROLE);
		}
		if (!StringUtils.hasText(request.phoneNumber())) {
			throw new BusinessException(ErrorCode.PHONE_NUMBER_REQUIRED);
		}

		user.changeRoleToAgent(request.phoneNumber());
		return new UserRoleResponse(user.getId(), user.getRole(), user.getPhoneNumber());
	}

	private User findUser(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
	}
}
