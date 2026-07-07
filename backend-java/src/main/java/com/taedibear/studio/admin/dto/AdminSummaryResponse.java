package com.taedibear.studio.admin.dto;

// Phase 5-1: 관리자 대시보드 요약 지표 (GET /api/admin/summary)
public record AdminSummaryResponse(
		Users users,
		Posts posts,
		Schedules schedules,
		Revenue revenue
) {
	public record Users(long total, long new_7d, long new_30d, long free, long pro) {}

	public record Posts(long total, long posted, long failed, long new_7d) {}

	public record Schedules(long pending, long failed) {}

	// month_revenue: 이번 달 1일 이후 PAID 합계(원), active_pro: 현재 유효 구독 수
	public record Revenue(long month_revenue, long active_pro) {}
}
