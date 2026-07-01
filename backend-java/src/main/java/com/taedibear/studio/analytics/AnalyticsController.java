package com.taedibear.studio.analytics;

import com.taedibear.studio.analytics.dto.AnalyticsSummaryResponse;
import com.taedibear.studio.common.ApiResponse;
import com.taedibear.studio.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @GetMapping("/summary")
    public ApiResponse<AnalyticsSummaryResponse> getSummary(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(analyticsService.getSummary(principal.getId()));
    }
}
