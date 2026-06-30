package com.taedibear.studio.schedule.dto;

import java.time.LocalDateTime;

public record ScheduleResponse(Long id, Long post_id, LocalDateTime scheduled_at, String status) {
}
