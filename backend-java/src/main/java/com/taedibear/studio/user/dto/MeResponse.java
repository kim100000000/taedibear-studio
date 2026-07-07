package com.taedibear.studio.user.dto;

import com.taedibear.studio.domain.User;

import java.time.LocalDateTime;

// Phase 2-3: business_type, mood 추가 / Phase 2-1: credits 추가 / Phase 5-1: is_admin 추가
public record MeResponse(Long id, String name, String email, String plan,
                         String business_type, String mood, int credits, LocalDateTime created_at,
                         boolean is_admin) {
	public static MeResponse from(User user) {
		return from(user, false);
	}

	public static MeResponse from(User user, boolean isAdmin) {
		return new MeResponse(user.getId(), user.getName(), user.getEmail(), user.getPlan().name(),
				user.getBusinessType(), user.getMood(), user.getCredits(), user.getCreatedAt(), isAdmin);
	}
}
