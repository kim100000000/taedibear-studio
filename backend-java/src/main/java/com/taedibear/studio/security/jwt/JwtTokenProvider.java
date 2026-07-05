package com.taedibear.studio.security.jwt;

import com.taedibear.studio.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Node 버전(jsonwebtoken)의 signToken/verify와 동일한 역할.
 * payload: { sub: userId, email }
 */
@Component
public class JwtTokenProvider {

	private final SecretKey key;
	private final long expirationMs;

	public JwtTokenProvider(
			@Value("${app.jwt.secret:}") String secret,
			@Value("${app.jwt.expiration-ms}") long expirationMs) {
		// C8: 기본값·짧은 키 금지. 미설정이거나 32바이트 미만이면 부팅을 실패시켜
		// 약한 키로 서비스가 뜨는 것을 원천 차단한다 (fail-fast).
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException(
					"JWT_SECRET 환경변수가 설정되지 않았어요. 'openssl rand -base64 48'로 생성한 값을 .env에 설정해주세요.");
		}
		if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
			throw new IllegalStateException(
					"JWT_SECRET이 너무 짧아요 (HS256은 최소 32바이트 필요). 'openssl rand -base64 48'로 생성한 값을 사용해주세요.");
		}
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.expirationMs = expirationMs;
	}

	public String generateToken(User user) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + expirationMs);

		return Jwts.builder()
				.subject(String.valueOf(user.getId()))
				.claim("email", user.getEmail())
				.issuedAt(now)
				.expiration(expiry)
				.signWith(key)
				.compact();
	}

	public Claims parseClaims(String token) {
		return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
	}

	// C4: parseClaimsIgnoringExpiration 제거 — 만료 무시 재발급은 refresh token 체계로 대체됨.

	public Long getUserId(Claims claims) {
		return Long.parseLong(claims.getSubject());
	}
}
