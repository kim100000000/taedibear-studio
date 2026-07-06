package com.taedibear.studio.analytics;

import com.taedibear.studio.analytics.dto.AnalyticsSummaryResponse;
import com.taedibear.studio.analytics.dto.AnalyticsSummaryResponse.DailyPattern;
import com.taedibear.studio.analytics.dto.AnalyticsSummaryResponse.FollowerPoint;
import com.taedibear.studio.analytics.dto.AnalyticsSummaryResponse.MonthlyUpload;
import com.taedibear.studio.repository.FollowerSnapshotRepository;
import com.taedibear.studio.repository.PostRepository;
import com.taedibear.studio.repository.ScheduledPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * 분석 대시보드 서비스 — 자체 DB 데이터 기반 (Meta API 없음)
 * docs/02_기능명세서.md F-17 참조
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final PostRepository postRepository;
    private final ScheduledPostRepository scheduledPostRepository;
    private final FollowerSnapshotRepository followerSnapshotRepository;

    // 요일 인덱스 (MySQL DAYOFWEEK: 1=일, 2=월, ..., 7=토)
    private static final String[] DAY_LABELS = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse getSummary(Long userId) {
        return getSummary(userId, null);
    }

    // Phase 4-1: instagramAccountId가 null이면 전체 계정 합산, 지정하면 해당 계정만 집계
    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse getSummary(Long userId, Long instagramAccountId) {
        return new AnalyticsSummaryResponse(
                buildMonthlyUploads(userId, instagramAccountId),
                buildSuccessRate(userId, instagramAccountId),
                buildScheduledRatio(userId, instagramAccountId),
                buildDailyPattern(userId, instagramAccountId),
                countThisMonth(userId, instagramAccountId),
                buildFollowerTrend(userId, instagramAccountId)
        );
    }

    /** Phase 4-2: 최근 30일 팔로워 추이 (계정 미지정 시 사용자의 전체 계정 합산) */
    private List<FollowerPoint> buildFollowerTrend(Long userId, Long accountId) {
        LocalDate from = LocalDate.now().minusDays(30);
        List<Object[]> rows = followerSnapshotRepository.findTrend(userId, accountId, from);
        List<FollowerPoint> result = new ArrayList<>();
        for (Object[] row : rows) {
            LocalDate date = (LocalDate) row[0];
            long followers = ((Number) row[1]).longValue();
            result.add(new FollowerPoint(date.toString(), followers));
        }
        return result;
    }

    /** 최근 12개월 월별 업로드 수 */
    private List<MonthlyUpload> buildMonthlyUploads(Long userId, Long accountId) {
        // JPQL: YEAR(), MONTH() 사용
        List<Object[]> rows = postRepository.findMonthlyUploadCounts(userId, accountId);

        // 최근 12개월 슬롯 초기화 (빈 달도 0으로 표시)
        Map<String, Long> map = new LinkedHashMap<>();
        YearMonth now = YearMonth.now();
        for (int i = 11; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            map.put(String.format("%04d-%02d", ym.getYear(), ym.getMonthValue()), 0L);
        }

        for (Object[] row : rows) {
            int year  = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            long cnt  = ((Number) row[2]).longValue();
            String key = String.format("%04d-%02d", year, month);
            if (map.containsKey(key)) map.put(key, cnt);
        }

        return map.entrySet().stream()
                .map(e -> new MonthlyUpload(e.getKey(), e.getValue()))
                .toList();
    }

    /** 성공률: posted / (posted + failed) */
    private double buildSuccessRate(Long userId, Long accountId) {
        long posted = postRepository.countByUserIdAndStatusName(userId, accountId, "posted");
        long failed = postRepository.countByUserIdAndStatusName(userId, accountId, "failed");
        long total  = posted + failed;
        return total == 0 ? 0.0 : Math.round((double) posted / total * 1000) / 10.0;
    }

    /** 예약 업로드 비율: 예약 경험이 있는 post / 전체 post */
    private double buildScheduledRatio(Long userId, Long accountId) {
        long total     = postRepository.countByUserIdFiltered(userId, accountId);
        long scheduled = scheduledPostRepository.countScheduledPostsByUserId(userId, accountId);
        return total == 0 ? 0.0 : Math.round((double) scheduled / total * 1000) / 10.0;
    }

    /** 요일별 업로드 패턴 (posted 상태, posted_at 기준) */
    private List<DailyPattern> buildDailyPattern(Long userId, Long accountId) {
        List<Object[]> rows = postRepository.findDailyPatternCounts(userId, accountId);

        // 초기화: 일~토 모두 0
        long[] counts = new long[7];
        for (Object[] row : rows) {
            // MySQL DAYOFWEEK: 1=일, 2=월, ... 7=토
            int dow = ((Number) row[0]).intValue(); // 1~7
            long cnt = ((Number) row[1]).longValue();
            if (dow >= 1 && dow <= 7) counts[dow - 1] = cnt;
        }

        List<DailyPattern> result = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            result.add(new DailyPattern(DAY_LABELS[i], counts[i]));
        }
        return result;
    }

    /** 이번 달 업로드 수 */
    private long countThisMonth(Long userId, Long accountId) {
        LocalDate now = LocalDate.now();
        return postRepository.countThisMonth(userId, accountId, now.getYear(), now.getMonthValue());
    }
}
