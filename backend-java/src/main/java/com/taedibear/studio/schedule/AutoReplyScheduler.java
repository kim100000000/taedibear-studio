package com.taedibear.studio.schedule;

import com.taedibear.studio.domain.InstagramAccount;
import com.taedibear.studio.repository.InstagramAccountRepository;
import com.taedibear.studio.review.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Phase 4-3: 30분마다 — "자동 발행"을 켜둔 계정의 새 댓글을 동기화하고 AI 답글을 자동으로 발행한다.
 * "승인 후 발행"으로 설정된 계정은 이 스케줄러가 건드리지 않고, 사용자가 리뷰 화면에서 직접 승인해야 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoReplyScheduler {

	private final InstagramAccountRepository instagramAccountRepository;
	private final ReviewService reviewService;

	@Scheduled(cron = "0 0/30 * * * *")
	public void autoReplyDueComments() {
		int processed = 0;
		for (InstagramAccount account : instagramAccountRepository.findAll()) {
			if (!account.isAutoReplyEnabled()) continue;
			try {
				reviewService.syncAndAutoReply(account);
				processed++;
			} catch (Exception ex) {
				log.warn("[자동 답글 스케줄러] accountId={} 처리 실패 — {}", account.getId(), ex.getMessage());
			}
		}
		if (processed > 0) {
			log.info("[자동 답글 스케줄러] {}개 계정 처리", processed);
		}
	}
}
