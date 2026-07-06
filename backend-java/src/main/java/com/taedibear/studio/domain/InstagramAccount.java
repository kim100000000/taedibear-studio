package com.taedibear.studio.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// docs/04_DB설계서.md 3.2 instagram_accounts
@Entity
@Table(name = "instagram_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstagramAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "instagram_user_id", nullable = false, length = 100)
	private String instagramUserId;

	@Column(length = 100)
	private String username;

	@Lob
	@Column(name = "access_token", nullable = false, columnDefinition = "TEXT")
	private String accessToken;

	@Column(name = "token_expires_at")
	private LocalDateTime tokenExpiresAt;

	// Phase 4-3: 리뷰(댓글) 답글을 Gemini가 자동으로 발행할지, 사용자 승인 후 발행할지 설정
	@Column(name = "auto_reply_enabled", nullable = false)
	@Builder.Default
	private boolean autoReplyEnabled = false;

	@CreationTimestamp
	@Column(name = "connected_at", updatable = false)
	private LocalDateTime connectedAt;
}
