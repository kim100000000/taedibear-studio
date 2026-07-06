package com.taedibear.studio.post.dto;

// Phase 4-2: GET /api/posts/:id/insights 응답
public record PostInsightsResponse(
		long engagement,
		long impressions,
		long reach,
		long like_count,
		long comments_count
) {
}
