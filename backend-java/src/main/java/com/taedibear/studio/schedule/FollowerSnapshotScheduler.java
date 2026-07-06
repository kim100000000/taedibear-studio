package com.taedibear.studio.schedule;

import com.taedibear.studio.domain.FollowerSnapshot;
import com.taedibear.studio.domain.InstagramAccount;
import com.taedibear.studio.instagram.MetaApiClient;
import com.taedibear.studio.repository.FollowerSnapshotRepository;
import com.taedibear.studio.repository.InstagramAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Phase 4-2: 매일 새벽 3시 — 연동된 모든 인스타그램 계정의 팔로워 수를 스냅샷으로 저장한다.
 * Meta Insights API는 팔로워 수 추이를 직접 제공하지 않으므로 매일 값을 쌓아 분석 대시보드의
 * "팔로워 증가 추이" 차트에 사용한다.
 *
 * 주의: instagram_manage_insights 권한이 없는(과거에 연동해 재연동하지 않은) 계정은
 * Meta API 호출이 실패할 수 있어 개별적으로 catch하고 다음 계정으로 넘어간다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FollowerSnapshotScheduler {

	private final InstagramAccountRepository instagramAccountRepository;
	private final FollowerSnapshotRepository followerSnapshotRepository;
	private final MetaApiClient metaApiClient;

	@Scheduled(cron = "0 0 3 * * *")
	public void snapshotFollowers() {
		LocalDate today = LocalDate.now();
		int success = 0, failed = 0;
		for (InstagramAccount account : instagramAccountRepository.findAll()) {
			try {
				long count = metaApiClient.getFollowersCount(account.getInstagramUserId(), account.getAccessToken());
				saveSnapshot(account.getId(), count, today);
				success++;
			} catch (Exception ex) {
				failed++;
				log.warn("[팔로워 스냅샷 실패] accountId={} — {}", account.getId(), ex.getMessage());
			}
		}
		log.info("[팔로워 스냅샷] 성공 {}건 / 실패 {}건", success, failed);
	}

	@Transactional
	protected void saveSnapshot(Long accountId, long followersCount, LocalDate today) {
		FollowerSnapshot snapshot = followerSnapshotRepository
				.findByInstagramAccountIdAndSnapshotDate(accountId, today)
				.orElseGet(() -> FollowerSnapshot.builder().instagramAccountId(accountId).snapshotDate(today).build());
		snapshot.setFollowersCount(followersCount);
		followerSnapshotRepository.save(snapshot);
	}
}
