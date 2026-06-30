package com.taedibear.studio.schedule.dto;

import java.time.LocalDateTime;

public record CreateScheduleRequest(Long post_id, LocalDateTime scheduled_at) {
}
