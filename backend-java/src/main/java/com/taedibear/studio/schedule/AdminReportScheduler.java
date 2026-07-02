package com.taedibear.studio.schedule;

import com.taedibear.studio.domain.PostStatus;
import com.taedibear.studio.domain.ScheduleStatus;
import com.taedibear.studio.email.EmailService;
import com.taedibear.studio.repository.PostRepository;
import com.taedibear.studio.repository.ScheduledPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Phase 2-3 관리자용: 매일 오전 9시 전날 실패 현황 요약 이메일 발송.
 * 실패 0건이면 발송하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminReportScheduler {

    private final PostRepository postRepository;
    private final ScheduledPostRepository scheduledPostRepository;
    private final EmailService emailService;

    @Scheduled(cron = "0 0 9 * * *")
    public void sendDailyReport() {
        // 전날 00:00 ~ 23:59:59 범위
        LocalDateTime startOfYesterday = LocalDateTime.now().minusDays(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfYesterday = startOfYesterday
                .withHour(23).withMinute(59).withSecond(59);

        // scheduled_posts 중 어제 처리된 failed 건수
        long finalFailed = scheduledPostRepository
                .countByStatusAndScheduledAtBetween(ScheduleStatus.failed, startOfYesterday, endOfYesterday);

        // posts 중 어제 posted 상태로 전환된 건수 (재시도 성공)
        long retrySuccess = postRepository
                .countByStatusAndPostedAtBetween(PostStatus.posted, startOfYesterday, endOfYesterday);

        // 어제 한 번이라도 실패가 발생한 총 건수 (failed + 재시도 성공)
        int totalFailed = (int) (finalFailed + retrySuccess);

        if (totalFailed == 0) {
            log.info("[AdminReport] 어제 실패 0건 — 이메일 발송 생략");
            return;
        }

        log.info("[AdminReport] 실패 현황 — 발생:{}건 / 재시도성공:{}건 / 최종실패:{}건",
                totalFailed, retrySuccess, finalFailed);
        emailService.sendAdminDailyReport(totalFailed, (int) retrySuccess, (int) finalFailed);
    }
}
