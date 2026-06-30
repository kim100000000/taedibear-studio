package com.taedibear.studio.repository;

import com.taedibear.studio.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByEmail(String email);

	Optional<User> findByGoogleId(String googleId);

	Optional<User> findByKakaoId(String kakaoId);

	Optional<User> findByNaverId(String naverId);

	boolean existsByEmail(String email);
}
