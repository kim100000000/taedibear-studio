package com.taedibear.studio.user.dto;

import com.taedibear.studio.domain.User;

import java.time.LocalDateTime;

// Phase 2-3: business_type, mood 추가 (캡션 페이지 초기값 자동 반영)
public record MeResponse(Long id, String name, String email, String plan,
                         String business_type, String mood, LocalDateTime created_at) {
	public static MeResponse from(User user) {
		return new MeResponse(user.getId(), user.getName(), user.getEmail(), user.getPlan().name(),
				user.getBusinessType(), user.getMood(), user.getCreatedAt());
	}
}
