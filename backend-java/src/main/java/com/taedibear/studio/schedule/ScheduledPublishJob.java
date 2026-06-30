package com.taedibear.studio.schedule;

import com.taedibear.studio.domain.*;
import com.taedibear.studio.instagram.InstagramPublishService;
import com.taedibear.studio.repository.InstagramAccountRepository;
import com.taedibear.studio.repository.PostRepository;
import com.taedibear.studio.repository.ScheduledPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Node 버전 services/cron.service.js#processDueSchedules / startScheduledPostCron 대응.
 * 1분마다 예약 시간이 지난 pending 건을 찾아 인스타그램에 발행한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledPublishJob {

	// retryCount < 3 인 건만 재시도 (docs/04_DB설계서.md scheduled_posts.retry_count)
	private static final int MAX_RETRY_COUNT = 3;

	// TODO(Meta API 연동 전 임시 처리, 8단계에서 제거): 실제 Meta 발행 전까지 instagram_post_id 자리만 채워둠.
	private static final String PLACEHOLDER_INSTAGRAM_POST_ID = "TODO-META-NOT-CONNECTED";

	private final ScheduledPostRepository scheduledPostRepository;
	private final PostRepository postRepository;
	private final InstagramAccountRepository instagramAccountRepository;
	private final InstagramPublishService instagramPublishService;

	@Scheduled(cron = "0 * * * * *")
	@Transactional
	public void processDueSchedules() {
		List<ScheduledPost> dueSchedules = scheduledPostRepository
				.findAllByStatusAndScheduledAtLessThanEqualAndRetryCountLessThan(
						ScheduleStatus.pending, LocalDateTime.now(), MAX_RETRY_COUNT);

		if (dueSchedules.isEmpty()) {
			return;
		}

		log.info("[Cron] 발행 대상 예약 {}건 조회됨", dueSchedules.size());

		for (ScheduledPost scheduledPost : dueSchedules) {
			processSchedule(scheduledPost);
		}
	}

	private void processSchedule(ScheduledPost scheduledPost) {
		Post post = postRepository.findById(scheduledPost.getPostId()).orElse(null);
		if (post == null) {
			log.warn("[Cron] schedule {}에 연결된 post {}를 찾을 수 없음", scheduledPost.getId(), scheduledPost.getPostId());
			scheduledPost.setStatus(ScheduleStatus.failed);
			return;
		}

		try {
			InstagramAccount account = instagramAccountRepository
					.findByIdAndUserId(post.getInstagramAccountId(), post.getUserId())
					.orElseThrow(() -> new IllegalStateException("연동된 인스타그램 계정을 찾을 수 없어요."));

			String instagramPostId = publish(post, account);

			post.setStatus(PostStatus.posted);
			post.setInstagramPostId(instagramPostId);
			post.setPostedAt(LocalDateTime.now());

			scheduledPost.setStatus(ScheduleStatus.done);

			log.info("[Cron] scheduled post {} published as {}", post.getId(), instagramPostId);
		} catch (Exception ex) {
			log.error("[Cron] scheduled post {} publish failed: {}", post.getId(), ex.getMessage());

			post.setStatus(PostStatus.failed);

			scheduledPost.setStatus(ScheduleStatus.failed);
			scheduledPost.setRetryCount(scheduledPost.getRetryCount() + 1);
		}
	}

	private String publish(Post post, InstagramAccount account) {
		// TODO(Meta API 연동 전 임시 처리, 8단계에서 교체): Meta 앱 심사가 끝나면 아래 한 줄로 교체.
		// return instagramPublishService.publishForSchedule(post, account);
		log.info("[Cron] (TODO) Meta API 미연동 - 실제 발행 대신 placeholder 처리. postId={}, igUserId={}",
				post.getId(), account.getInstagramUserId());
		return PLACEHOLDER_INSTAGRAM_POST_ID;
	}
}
