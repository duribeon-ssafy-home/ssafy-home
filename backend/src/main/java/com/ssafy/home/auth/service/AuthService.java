package com.ssafy.home.auth.service;

import com.ssafy.home.auth.dto.request.LoginRequest;
import com.ssafy.home.auth.dto.request.ForgotPasswordRequest;
import com.ssafy.home.auth.dto.request.RefreshTokenRequest;
import com.ssafy.home.auth.dto.request.SignupRequest;
import com.ssafy.home.auth.dto.response.AuthUserResponse;
import com.ssafy.home.auth.dto.response.LoginResponse;
import com.ssafy.home.auth.dto.response.TokenResponse;
import com.ssafy.home.auth.jwt.JwtProperties;
import com.ssafy.home.auth.jwt.JwtTokenProvider;
import com.ssafy.home.auth.refresh.RefreshToken;
import com.ssafy.home.auth.refresh.RefreshTokenRepository;
import com.ssafy.home.common.exception.BusinessException;
import com.ssafy.home.common.exception.ErrorCode;
import com.ssafy.home.user.entity.User;
import com.ssafy.home.user.repository.UserRepository;
import com.ssafy.home.user.type.AuthProvider;
import com.ssafy.home.user.type.Role;
import com.ssafy.home.user.type.UserStatus;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class AuthService {

	private static final String TOKEN_TYPE = "Bearer";
	private static final char[] TEMPORARY_PASSWORD_CHARS =
			"ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%".toCharArray();
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final JwtProperties jwtProperties;
	private final TemporaryPasswordMailService temporaryPasswordMailService;

	public AuthService(
			UserRepository userRepository,
			RefreshTokenRepository refreshTokenRepository,
			PasswordEncoder passwordEncoder,
			JwtTokenProvider jwtTokenProvider,
			JwtProperties jwtProperties,
			TemporaryPasswordMailService temporaryPasswordMailService
	) {
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenProvider = jwtTokenProvider;
		this.jwtProperties = jwtProperties;
		this.temporaryPasswordMailService = temporaryPasswordMailService;
	}

	@Transactional
	public AuthUserResponse signup(SignupRequest request) {
		validateSignup(request);

		User user = User.builder()
				.email(request.email())
				.password(passwordEncoder.encode(request.password()))
				.name(request.name())
				.nickname(request.nickname())
				.phoneNumber(request.phoneNumber())
				.role(request.role())
				.status(UserStatus.ACTIVE)
				.provider(AuthProvider.LOCAL)
				.build();

		return AuthUserResponse.from(userRepository.save(user));
	}

	@Transactional
	public LoginResponse login(LoginRequest request) {
		User user = userRepository.findByEmail(request.email())
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

		validateLogin(user, request.password());
		user.recordLogin();

		String accessToken = jwtTokenProvider.createAccessToken(user);
		String refreshToken = createRefreshToken(user);

		return new LoginResponse(
				accessToken,
				refreshToken,
				TOKEN_TYPE,
				jwtTokenProvider.getAccessTokenExpirationSeconds(),
				AuthUserResponse.from(user)
		);
	}

	@Transactional
	public void logout(RefreshTokenRequest request) {
		refreshTokenRepository.findByToken(request.refreshToken())
				.ifPresent(RefreshToken::revoke);
	}

	@Transactional
	public TokenResponse refresh(RefreshTokenRequest request) {
		RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

		if (!refreshToken.isValid()) {
			throw new BusinessException(ErrorCode.INVALID_TOKEN);
		}

		User user = refreshToken.getUser();
		validateAccountStatus(user);
		refreshToken.revoke();
		String newAccessToken = jwtTokenProvider.createAccessToken(user);
		String newRefreshToken = createRefreshToken(user);

		return new TokenResponse(
				newAccessToken,
				newRefreshToken,
				TOKEN_TYPE,
				jwtTokenProvider.getAccessTokenExpirationSeconds()
		);
	}

	@Transactional(readOnly = true)
	public AuthUserResponse getMe(Long userId) {
		return AuthUserResponse.from(findUser(userId));
	}

	@Transactional
	public void issueTemporaryPassword(ForgotPasswordRequest request) {
		userRepository.findByEmail(request.email())
				.filter(user -> user.getStatus() == UserStatus.ACTIVE)
				.ifPresent(user -> {
					String temporaryPassword = createTemporaryPassword();
					user.changePassword(passwordEncoder.encode(temporaryPassword));
					refreshTokenRepository.deleteAllByUser(user);
					temporaryPasswordMailService.send(user, temporaryPassword);
				});
	}

	private void validateSignup(SignupRequest request) {
		if (request.role() == Role.ADMIN) {
			throw new BusinessException(ErrorCode.INVALID_ROLE);
		}
		if (request.role() == Role.AGENT && !StringUtils.hasText(request.phoneNumber())) {
			throw new BusinessException(ErrorCode.PHONE_NUMBER_REQUIRED);
		}
		if (userRepository.existsByEmail(request.email())) {
			throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
		}
	}

	private void validateLogin(User user, String rawPassword) {
		if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
			throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
		}
		validateAccountStatus(user);
	}

	private void validateAccountStatus(User user) {
		if (user.getStatus() == UserStatus.INACTIVE) {
			throw new BusinessException(ErrorCode.INACTIVE_USER);
		}
		if (user.getStatus() == UserStatus.BANNED) {
			throw new BusinessException(ErrorCode.BANNED_USER);
		}
		if (user.getStatus() == UserStatus.DELETED) {
			throw new BusinessException(ErrorCode.DELETED_USER);
		}
	}

	private String createRefreshToken(User user) {
		byte[] bytes = new byte[64];
		SECURE_RANDOM.nextBytes(bytes);
		String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(jwtProperties.refreshTokenExpirationSeconds());
		refreshTokenRepository.save(new RefreshToken(user, token, expiresAt));
		return token;
	}

	private String createTemporaryPassword() {
		StringBuilder password = new StringBuilder(12);
		for (int i = 0; i < 12; i++) {
			password.append(TEMPORARY_PASSWORD_CHARS[SECURE_RANDOM.nextInt(TEMPORARY_PASSWORD_CHARS.length)]);
		}
		return password.toString();
	}

	private User findUser(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
	}
}
