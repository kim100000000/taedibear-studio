package com.taedibear.studio.security.jwt;

import com.taedibear.studio.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
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
			@Value("${app.jwt.secret}") String secret,
			@Value("${app.jwt.expiration-ms}") long expirationMs) {
		// HS256은 최소 256bit(32byte) 키가 필요하다. 운영 환경에서는 JWT_SECRET을 충분히 긴 값으로 설정해야 한다.
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

	// POST /api/auth/refresh: 만료된 토큰도 허용해서 새 토큰을 발급한다 (서명만 검증, 만료는 무시).
	public Claims parseClaimsIgnoringExpiration(String token) {
		try {
			return parseClaims(token);
		} catch (ExpiredJwtException ex) {
			return ex.getClaims();
		}
	}

	public Long getUserId(Claims claims) {
		return Long.parseLong(claims.getSubject());
	}
}
