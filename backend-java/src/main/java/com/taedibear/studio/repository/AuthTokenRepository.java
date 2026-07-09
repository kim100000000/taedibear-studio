package com.taedibear.studio.repository;

import com.taedibear.studio.domain.AuthToken;
import com.taedibear.studio.domain.AuthTokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

	Optional<AuthToken> findByTokenHashAndType(String tokenHash, AuthTokenType type);

	// 재발급 시 이전 토큰 폐기 (사용자당 용도별 유효 토큰은 항상 1개)
	@Modifying
	void deleteAllByUserIdAndType(Long userId, AuthTokenType type);

	// 회원 탈퇴 시 정리
	@Modifying
	void deleteAllByUserId(Long userId);

	@Modifying
	@Query("delete from AuthToken t where t.expiresAt < :now")
	int deleteAllExpired(@Param("now") LocalDateTime now);
}
