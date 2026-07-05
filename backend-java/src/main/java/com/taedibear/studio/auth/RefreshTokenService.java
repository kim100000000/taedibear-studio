package com.taedibear.studio.auth;

import com.taedibear.studio.common.exception.ApiException;
import com.taedibear.studio.domain.RefreshToken;
import com.taedibear.studio.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * C4: refresh token 발급/회전/폐기.
 * - 원문은 256비트 랜덤 → 클라이언트에 HttpOnly 쿠키로만 전달 (AuthController)
 * - DB에는 SHA-256 해시만 저장
 * - 사용(회전) 시 기존 토큰을 삭제하고 새 토큰 발급 → 탈취된 토큰의 재사용을 차단하고,
 *   이미 회전된 토큰이 다시 들어오면(재사용 감지) 로그인 요구
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

	private static final SecureRandom RANDOM = new SecureRandom();

	private final RefreshTokenRepository refreshTokenRepository;

	@Value("${app.auth.refresh-expiration-ms}")
	private long refreshExpirationMs;

	// 회전 결과: 새 access token 발급에 필요한 userId + 쿠키로 내려줄 새 refresh token 원문
	public record RotationResult(Long userId, String newRefreshToken) {
	}

	@Transactional
	public String issue(Long userId) {
		String raw = generateRawToken();
		refreshTokenRepository.save(RefreshToken.builder()
				.userId(userId)
				.tokenHash(sha256(raw))
				.expiresAt(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000))
				.build());
		return raw;
	}

	@Transactional
	public RotationResult rotate(String rawToken) {
		RefreshToken stored = refreshTokenRepository.findByTokenHash(sha256(rawToken))
				.orElseThrow(() -> ApiException.unauthorized("세션이 만료됐어요. 다시 로그인해주세요."));

		if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
			refreshTokenRepository.delete(stored);
			throw ApiException.unauthorized("세션이 만료됐어요. 다시 로그인해주세요.");
		}

		// 회전: 사용한 토큰은 즉시 폐기하고 새로 발급 (일회성)
		Long userId = stored.getUserId();
		refreshTokenRepository.delete(stored);
		return new RotationResult(userId, issue(userId));
	}

	// 로그아웃: 쿠키의 토큰만 폐기 (다른 기기 세션은 유지)
	@Transactional
	public void revoke(String rawToken) {
		refreshTokenRepository.deleteByTokenHash(sha256(rawToken));
	}

	// 회원 탈퇴: 해당 사용자의 모든 세션 폐기
	@Transactional
	public void revokeAllForUser(Long userId) {
		refreshTokenRepository.deleteAllByUserId(userId);
	}

	// 만료 토큰 일일 정리 (매일 새벽 4시)
	@Scheduled(cron = "0 0 4 * * *")
	@Transactional
	public void cleanUpExpired() {
		int deleted = refreshTokenRepository.deleteAllExpired(LocalDateTime.now());
		if (deleted > 0) {
			log.info("[RefreshToken cleanup] 만료 토큰 {}건 삭제", deleted);
		}
	}

	private String generateRawToken() {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String sha256(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException ex) {
			// SHA-256은 모든 JVM에 필수 탑재 — 발생할 수 없음
			throw new IllegalStateException("SHA-256을 사용할 수 없어요.", ex);
		}
	}
}
