package com.taedibear.studio.analytics;

import com.taedibear.studio.analytics.dto.AnalyticsSummaryResponse;
import com.taedibear.studio.common.ApiResponse;
import com.taedibear.studio.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/analytics/summary
 * docs/05_API명세서.md 6. 분석 API
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // Phase 4-1: instagram_account_id 쿼리 파라미터로 계정별 통계 분리 조회 (없으면 전체 계정 합산)
    @GetMapping("/summary")
    public ApiResponse<AnalyticsSummaryResponse> getSummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long instagram_account_id) {
        return ApiResponse.ok(analyticsService.getSummary(principal.getId(), instagram_account_id));
    }
}
