package com.taedibear.studio.admin.dto;

import java.util.List;

// Phase 5-1: 관리자 목록 공통 페이지 응답 포맷
public record AdminPageResponse<T>(
		List<T> items,
		int page,
		int size,
		long total,
		int total_pages
) {
	public static <T> AdminPageResponse<T> of(List<T> items, int page, int size, long total) {
		int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
		return new AdminPageResponse<>(items, page, size, total, totalPages);
	}
}
