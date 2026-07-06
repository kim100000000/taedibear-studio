package com.taedibear.studio.review.dto;

import java.time.LocalDateTime;

// Phase 4-3: GET /api/instagram/accounts/:id/comments 응답 아이템
public record CommentResponse(
		Long id,
		String media_id,
		String comment_text,
		String username,
		String suggested_reply,
		String status,
		LocalDateTime created_at
) {
}
