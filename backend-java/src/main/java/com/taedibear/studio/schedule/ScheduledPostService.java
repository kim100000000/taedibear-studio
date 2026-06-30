package com.taedibear.studio.schedule;

import com.taedibear.studio.common.exception.ApiException;
import com.taedibear.studio.domain.Post;
import com.taedibear.studio.domain.PostStatus;
import com.taedibear.studio.domain.ScheduleStatus;
import com.taedibear.studio.domain.ScheduledPost;
import com.taedibear.studio.repository.PostRepository;
import com.taedibear.studio.repository.ScheduledPostRepository;
import com.taedibear.studio.schedule.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduledPostService {

	// docs/05_API명세서.md: scheduled_at은 현재 시간 + 10분 이후여야 함
	private static final Duration MIN_LEAD = Duration.ofMinutes(10);

	private final PostRepository postRepository;
	private final ScheduledPostRepository scheduledPostRepository;

	private void validateLeadTime(LocalDateTime scheduledAt) {
		if (scheduledAt == null || scheduledAt.isBefore(LocalDateTime.now().plus(MIN_LEAD))) {
			throw ApiException.badRequest("예약 시간은 현재 시간으로부터 10분 이후여야 해요.");
		}
	}

	@Transactional
	public ScheduleResponse createSchedule(Long userId, CreateScheduleRequest request) {
		if (request.post_id() == null || request.scheduled_at() == null) {
			throw ApiException.badRequest("post_id, scheduled_at은 필수예요.");
		}
		validateLeadTime(request.scheduled_at());

		Post post = postRepository.findByIdAndUserId(request.post_id(), userId)
				.orElseThrow(() -> ApiException.notFound("게시물을 찾을 수 없어요."));
		if (post.getStatus() == PostStatus.posted) {
			throw ApiException.badRequest("업로드 완료된 게시물은 예약할 수 없어요.");
		}

		ScheduledPost scheduledPost = scheduledPostRepository.save(ScheduledPost.builder()
				.postId(post.getId())
				.scheduledAt(request.scheduled_at())
				.status(ScheduleStatus.pending)
				.retryCount(0)
				.build());

		post.setStatus(PostStatus.scheduled);

		return new ScheduleResponse(scheduledPost.getId(), post.getId(), scheduledPost.getScheduledAt(),
				scheduledPost.getStatus().name());
	}

	public List<ScheduleListItemResponse> listSchedules(Long userId) {
		return scheduledPostRepository.findAllByPostUserId(userId).stream()
				.map(sp -> {
					Post post = postRepository.findById(sp.getPostId()).orElse(null);
					var summary = post != null
							? new ScheduleListItemResponse.PostSummary(post.getImageUrl(), post.getCaption())
							: new ScheduleListItemResponse.PostSummary(null, null);
					return new ScheduleListItemResponse(
							sp.getId(), sp.getPostId(), sp.getScheduledAt(), sp.getStatus().name(), summary);
				})
				.toList();
	}

	@Transactional
	public ScheduleResponse updateSchedule(Long scheduleId, Long userId, UpdateScheduleRequest request) {
		if (request.scheduled_at() == null) {
			throw ApiException.badRequest("scheduled_at은 필수예요.");
		}
		validateLeadTime(request.scheduled_at());

		ScheduledPost scheduledPost = scheduledPostRepository.findByIdAndPostUserId(scheduleId, userId)
				.orElseThrow(() -> ApiException.notFound("예약을 찾을 수 없어요."));
		if (scheduledPost.getStatus() != ScheduleStatus.pending) {
			throw ApiException.badRequest("대기 중인 예약만 수정할 수 있어요.");
		}

		scheduledPost.setScheduledAt(request.scheduled_at());
		return new ScheduleResponse(scheduledPost.getId(), scheduledPost.getPostId(),
				scheduledPost.getScheduledAt(), scheduledPost.getStatus().name());
	}

	@Transactional
	public void deleteSchedule(Long scheduleId, Long userId) {
		ScheduledPost scheduledPost = scheduledPostRepository.findByIdAndPostUserId(scheduleId, userId)
				.orElseThrow(() -> ApiException.notFound("예약을 찾을 수 없어요."));

		Post post = postRepository.findById(scheduledPost.getPostId()).orElse(null);
		scheduledPostRepository.delete(scheduledPost);

		if (post != null && post.getStatus() == PostStatus.scheduled) {
			post.setStatus(PostStatus.draft);
		}
	}
}
