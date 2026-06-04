package com.ssafy.home.user.repository;

import com.ssafy.home.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

	boolean existsByEmail(String email);

	Optional<User> findByEmail(String email);

	List<User> findByEmailContainingIgnoreCaseOrNameContainingIgnoreCaseOrNicknameContainingIgnoreCase(
			String email,
			String name,
			String nickname
	);
}
