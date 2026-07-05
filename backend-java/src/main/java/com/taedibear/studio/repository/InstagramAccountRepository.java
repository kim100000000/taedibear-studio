package com.taedibear.studio.repository;

import com.taedibear.studio.domain.InstagramAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InstagramAccountRepository extends JpaRepository<InstagramAccount, Long> {
	List<InstagramAccount> findAllByUserId(Long userId);

	Optional<InstagramAccount> findByUserIdAndInstagramUserId(Long userId, String instagramUserId);

	Optional<InstagramAccount> findByIdAndUserId(Long id, Long userId);

	// 회원 탈퇴: 사용자의 모든 인스타 연동 삭제 (posts 삭제 후 실행 — posts가 이 계정을 참조하므로)
	@Modifying
	@Query("delete from InstagramAccount a where a.userId = :userId")
	void deleteAllByUserId(@Param("userId") Long userId);
}
