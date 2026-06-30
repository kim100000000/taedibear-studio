package com.taedibear.studio.schedule.dto;

import java.time.LocalDateTime;

public record UpdateScheduleRequest(LocalDateTime scheduled_at) {
}
