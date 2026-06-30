package com.taedibear.studio.post.dto;

import java.time.LocalDateTime;

public record PostListItemResponse(
		Long id,
		String image_url,
		String caption,
		String status,
		LocalDateTime posted_at,
		LocalDateTime scheduled_at
) {
}
