package com.taedibear.studio.post.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PostDetailResponse(
		Long id,
		String image_url,
		String caption,
		List<String> hashtags,
		String status,
		String instagram_post_id,
		LocalDateTime posted_at
) {
}
