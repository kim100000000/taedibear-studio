package com.taedibear.studio.repository;

import com.taedibear.studio.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	// 회전/로그아웃 시 사용한 토큰 즉시 폐기
	@Modifying
	@Query("delete from RefreshToken r where r.tokenHash = :tokenHash")
	void deleteByTokenHash(@Param("tokenHash") String tokenHash);

	// 회원 탈퇴 시 해당 사용자의 모든 refresh token 폐기
	@Modifying
	@Query("delete from RefreshToken r where r.userId = :userId")
	void deleteAllByUserId(@Param("userId") Long userId);

	// 만료 토큰 정리 (일일 스케줄)
	@Modifying
	@Query("delete from RefreshToken r where r.expiresAt < :now")
	int deleteAllExpired(@Param("now") LocalDateTime now);
}
