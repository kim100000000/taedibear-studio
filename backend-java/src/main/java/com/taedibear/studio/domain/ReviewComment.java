package com.taedibear.studio.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// Phase 4-3: 리뷰(댓글) 자동 답글 — Meta에서 가져온 댓글을 로컬에 기록해 중복 답글을 방지하고
// 승인 후 발행 / 자동 발행 상태를 추적한다.
@Entity
@Table(name = "review_comments", uniqueConstraints = {
		@UniqueConstraint(columnNames = {"comment_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewComment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "instagram_account_id", nullable = false)
	private Long instagramAccountId;

	@Column(name = "media_id", nullable = false, length = 100)
	private String mediaId;

	// Instagram 댓글 ID (Meta 쪽 고유 ID) — 중복 동기화 방지용 유니크 키
	@Column(name = "comment_id", nullable = false, length = 100)
	private String commentId;

	@Lob
	@Column(name = "comment_text", columnDefinition = "TEXT")
	private String commentText;

	@Column(length = 100)
	private String username;

	@Lob
	@Column(name = "suggested_reply", columnDefinition = "TEXT")
	private String suggestedReply;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private ReviewStatus status = ReviewStatus.pending;

	@Column(name = "posted_reply_id", length = 100)
	private String postedReplyId;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;
}
