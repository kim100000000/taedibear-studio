package com.taedibear.studio.auth;

import com.taedibear.studio.common.exception.ApiException;
import com.taedibear.studio.domain.AuthToken;
import com.taedibear.studio.domain.AuthTokenType;
import com.taedibear.studio.repository.AuthTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Phase 6: 비밀번호 재설정 / 이메일 인증 토큰 발급·검증.
 * RefreshTokenService와 동일한 패턴 — 원문 256비트 랜덤, DB에는 SHA-256 해시만.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthTokenService {

	private static final SecureRandom RANDOM = new SecureRandom();

	private final AuthTokenRepository authTokenRepository;

	/** 토큰 발급 — 같은 용도의 기존 토큰은 폐기하고 새로 발급 (가장 최근 메일 링크만 유효) */
	@Transactional
	public String issue(Long userId, AuthTokenType type, Duration ttl) {
		authTokenRepository.deleteAllByUserIdAndType(userId, type);
		String raw = generateRawToken();
		authTokenRepository.save(AuthToken.builder()
				.userId(userId)
				.tokenHash(sha256(raw))
				.type(type)
				.expiresAt(LocalDateTime.now().plus(ttl))
				.build());
		return raw;
	}

	/** 토큰 검증 + 즉시 폐기(일회성). 유효하지 않으면 400. @return 토큰 소유자 userId */
	@Transactional
	public Long consume(String rawToken, AuthTokenType type) {
		AuthToken stored = authTokenRepository.findByTokenHashAndType(sha256(rawToken), type)
				.orElseThrow(() -> ApiException.badRequest("유효하지 않거나 만료된 링크예요. 다시 요청해주세요."));

		authTokenRepository.delete(stored);
		if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
			throw ApiException.badRequest("유효하지 않거나 만료된 링크예요. 다시 요청해주세요.");
		}
		return stored.getUserId();
	}

	// 만료 토큰 일일 정리 (매일 새벽 4시 10분 — refresh token 정리와 시차)
	@Scheduled(cron = "0 10 4 * * *")
	@Transactional
	public void cleanUpExpired() {
		int deleted = authTokenRepository.deleteAllExpired(LocalDateTime.now());
		if (deleted > 0) {
			log.info("[AuthToken cleanup] 만료 토큰 {}건 삭제", deleted);
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
			throw new IllegalStateException("SHA-256을 사용할 수 없어요.", ex);
		}
	}
}
