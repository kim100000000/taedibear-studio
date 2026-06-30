package com.taedibear.studio.user.dto;

import com.taedibear.studio.domain.User;

import java.time.LocalDateTime;

public record MeResponse(Long id, String name, String email, String plan, LocalDateTime created_at) {
	public static MeResponse from(User user) {
		return new MeResponse(user.getId(), user.getName(), user.getEmail(), user.getPlan().name(), user.getCreatedAt());
	}
}
