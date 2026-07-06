package com.taedibear.studio.post.dto;

import java.time.LocalDateTime;

public record PostListItemResponse(
		Long id,
		Long instagram_account_id,
		String image_url,
		String caption,
		String status,
		String instagram_post_id,
		LocalDateTime posted_at,
		LocalDateTime scheduled_at
) {
}
