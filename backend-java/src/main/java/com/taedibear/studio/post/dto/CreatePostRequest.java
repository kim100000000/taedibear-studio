package com.taedibear.studio.post.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreatePostRequest(
		@NotNull(message = "instagram_account_id는 필수예요.") Long instagram_account_id,
		@NotNull(message = "image_url은 필수예요.") String image_url,
		String caption,
		List<String> hashtags
) {
}
