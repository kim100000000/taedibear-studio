package com.taedibear.studio.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Phase 6: 비밀번호 재설정 / 이메일 인증용 일회성 토큰.
 * refresh_tokens와 동일한 보안 원칙 — 원문은 이메일 링크로만 전달하고 DB에는 SHA-256 해시만 저장.
 * 사용(consume) 시 즉시 삭제되는 일회성이며, 같은 용도 토큰을 재발급하면 이전 토큰은 폐기된다.
 */
@Entity
@Table(name = "auth_tokens", indexes = {
		@Index(name = "idx_auth_tokens_token_hash", columnList = "token_hash", unique = true),
		@Index(name = "idx_auth_tokens_user_id", columnList = "user_id")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	// SHA-256 해시 (hex, 64자)
	@Column(name = "token_hash", nullable = false, length = 64)
	private String tokenHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AuthTokenType type;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;
}
