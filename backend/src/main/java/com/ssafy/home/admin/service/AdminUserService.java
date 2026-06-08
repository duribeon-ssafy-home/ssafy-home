package com.ssafy.home.admin.service;

import com.ssafy.home.common.exception.BusinessException;
import com.ssafy.home.common.exception.ErrorCode;
import com.ssafy.home.user.dto.request.UserRoleUpdateRequest;
import com.ssafy.home.user.dto.request.UserStatusUpdateRequest;
import com.ssafy.home.user.dto.response.UserResponse;
import com.ssafy.home.user.entity.User;
import com.ssafy.home.user.repository.UserRepository;
import com.ssafy.home.user.type.Role;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserRepository userRepository;

    public AdminUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserResponse> getUsers(String keyword) {
        List<User> users = StringUtils.hasText(keyword)
                ? userRepository.findByEmailContainingIgnoreCaseOrNameContainingIgnoreCaseOrNicknameContainingIgnoreCase(
                        keyword,
                        keyword,
                        keyword
                )
                : userRepository.findAll();

        return users.stream()
                .map(UserResponse::from)
                .toList();
    }

    public UserResponse getUser(Long userId) {
        return UserResponse.from(findUser(userId));
    }

    @Transactional
    public UserResponse updateUserStatus(Long userId, UserStatusUpdateRequest request) {
        User user = findUser(userId);
        user.changeStatus(request.status());
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateUserRole(Long userId, Long adminUserId, UserRoleUpdateRequest request) {
        User user = findUser(userId);
        validateRoleUpdate(user, adminUserId, request);
        user.changeRole(request.role(), request.phoneNumber());
        return UserResponse.from(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private void validateRoleUpdate(User user, Long adminUserId, UserRoleUpdateRequest request) {
        if (user.getId().equals(adminUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (user.getRole() == Role.ADMIN || request.role() == Role.ADMIN) {
            throw new BusinessException(ErrorCode.INVALID_ROLE);
        }
        if (request.role() == Role.AGENT
                && !StringUtils.hasText(request.phoneNumber())
                && !StringUtils.hasText(user.getPhoneNumber())) {
            throw new BusinessException(ErrorCode.PHONE_NUMBER_REQUIRED);
        }
    }
}
