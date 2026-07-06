package com.taedibear.studio.analytics.dto;

import java.util.List;

/**
 * GET /api/analytics/summary 응답 DTO
 * docs/05_API명세서.md 6. 분석 API
 */
public record AnalyticsSummaryResponse(
        List<MonthlyUpload> monthly_uploads,
        double success_rate,
        double scheduled_ratio,
        List<DailyPattern> daily_pattern,
        long this_month_used,
        List<FollowerPoint> follower_trend
) {
    /** 월별 업로드 수 (e.g. "2026-07") */
    public record MonthlyUpload(String month, long count) {}

    /** 요일별 업로드 수 (day: "Sun" ~ "Sat") */
    public record DailyPattern(String day, long count) {}

    /** Phase 4-2: 팔로워 추이 스냅샷 (date: "YYYY-MM-DD") */
    public record FollowerPoint(String date, long followers) {}
}
