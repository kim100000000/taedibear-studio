package com.taedibear.studio.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// C4: refresh token — DB 저장으로 회전(rotation)·폐기(revoke)가 가능한 토큰.
// 원문(raw) 토큰은 저장하지 않고 SHA-256 해시만 저장한다 → DB가 유출되어도 토큰 재사용 불가.
// user_id는 FK가 아닌 단순 컬럼(payments와 동일한 방식) — 탈퇴 시 deleteAllByUserId로 정리한다.
@Entity
@Table(name = "refresh_tokens", indexes = {
		@Index(name = "idx_refresh_tokens_token_hash", columnList = "token_hash", unique = true),
		@Index(name = "idx_refresh_tokens_user_id", columnList = "user_id")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	// SHA-256 해시 (hex, 64자)
	@Column(name = "token_hash", nullable = false, length = 64)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;
}
