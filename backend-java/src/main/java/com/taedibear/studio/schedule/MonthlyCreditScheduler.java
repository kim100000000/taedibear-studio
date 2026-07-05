package com.taedibear.studio.schedule;

import com.taedibear.studio.repository.UserRepository;
import com.taedibear.studio.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 매월 1일 00:05 — Free 플랜 사용자에게 무료 크레딧 3개 자동 지급.
 * 무료 사용자가 매달 다시 방문할 이유를 만드는 습관 장치 (재방문 → 전환).
 * 보유 상한(UserService.MAX_CREDITS)을 넘지 않게 지급하며, Pro는 크레딧을 쓰지 않으므로 제외.
 *
 * 주의: 다중 인스턴스 배포 시 중복 실행 가능 — 지급 쿼리가 LEAST(cap)라 상한을 넘진 않지만,
 * 확장 시 ShedLock 도입 검토 (ScheduledPublishJob과 동일한 이슈, 08 보고서 M3).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyCreditScheduler {

	public static final int MONTHLY_FREE_CREDITS = 3;

	private final UserRepository userRepository;

	@Scheduled(cron = "0 5 0 1 * *")
	@Transactional
	public void grantMonthlyCredits() {
		int granted = userRepository.grantMonthlyCredits(MONTHLY_FREE_CREDITS, UserService.MAX_CREDITS);
		log.info("[월간 크레딧 지급] Free 사용자 {}명에게 최대 {}개 지급 (상한 {})",
				granted, MONTHLY_FREE_CREDITS, UserService.MAX_CREDITS);
	}
}
