package com.taedibear.studio.schedule.dto;

import java.time.LocalDateTime;

public record ScheduleListItemResponse(
		Long id, Long post_id, LocalDateTime scheduled_at, String status, PostSummary post) {

	public record PostSummary(String image_url, String caption) {
	}
}
