package com.taedibear.studio.repository;

import com.taedibear.studio.domain.InstagramAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InstagramAccountRepository extends JpaRepository<InstagramAccount, Long> {
	List<InstagramAccount> findAllByUserId(Long userId);

	Optional<InstagramAccount> findByUserIdAndInstagramUserId(Long userId, String instagramUserId);

	Optional<InstagramAccount> findByIdAndUserId(Long id, Long userId);
}
