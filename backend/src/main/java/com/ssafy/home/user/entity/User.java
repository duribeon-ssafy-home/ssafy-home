package com.ssafy.home.user.entity;

import com.ssafy.home.common.entity.BaseTimeEntity;
import com.ssafy.home.user.type.AuthProvider;
import com.ssafy.home.user.type.Role;
import com.ssafy.home.user.type.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 100)
	private String email;

	@Column(length = 255)
	private String password;

	@Column(nullable = false, length = 50)
	private String name;

	@Column(nullable = false, length = 50)
	private String nickname;

	@Column(length = 20)
	private String phoneNumber;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private UserStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AuthProvider provider;

	@Column(length = 100)
	private String providerId;

	private LocalDateTime lastLoginAt;

	@Builder
	private User(
			String email,
			String password,
			String name,
			String nickname,
			String phoneNumber,
			Role role,
			UserStatus status,
			AuthProvider provider,
			String providerId
	) {
		this.email = email;
		this.password = password;
		this.name = name;
		this.nickname = nickname;
		this.phoneNumber = phoneNumber;
		this.role = role;
		this.status = status == null ? UserStatus.ACTIVE : status;
		this.provider = provider == null ? AuthProvider.LOCAL : provider;
		this.providerId = providerId;
	}

	public void updateProfile(String name, String nickname, String phoneNumber) {
		this.name = name;
		this.nickname = nickname;
		this.phoneNumber = phoneNumber;
	}

	public void changeStatus(UserStatus status) {
		this.status = status;
	}

	public void changeRoleToAgent(String phoneNumber) {
		this.role = Role.AGENT;
		this.phoneNumber = phoneNumber;
	}

	public void recordLogin() {
		this.lastLoginAt = LocalDateTime.now();
	}
}
