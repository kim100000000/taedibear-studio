package com.taedibear.studio.admin.dto;

import com.taedibear.studio.domain.User;

import java.time.LocalDateTime;

// Phase 5-1: 관리자 유저 목록 항목 (GET /api/admin/users)
public record AdminUserItemResponse(
		Long id,
		String name,
		String email,
		String plan,
		int credits,
		long post_count,
		LocalDateTime created_at
) {
	public static AdminUserItemResponse from(User user, long postCount) {
		return new AdminUserItemResponse(user.getId(), user.getName(), user.getEmail(),
				user.getPlan().name(), user.getCredits(), postCount, user.getCreatedAt());
	}
}
