package com.taedibear.studio.instagram;

import com.taedibear.studio.common.exception.ApiException;
import com.taedibear.studio.domain.InstagramAccount;
import com.taedibear.studio.domain.Post;
import com.taedibear.studio.domain.PostStatus;
import com.taedibear.studio.post.HashtagUtil;
import com.taedibear.studio.repository.InstagramAccountRepository;
import com.taedibear.studio.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;

/**
 * 즉시 발행(POST /api/posts/:id/publish)과 예약 발행(scheduled cron job)이 공유하는
 * 핵심 Instagram 업로드 로직. Node 버전의 posts.controller.js#publishPost,
 * services/cron.service.js#processDueSchedules 중복 코드를 한 곳으로 합쳤다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InstagramPublishService {

	private final PostRepository postRepository;
	private final InstagramAccountRepository instagramAccountRepository;
	private final MetaApiClient metaApiClient;

	public record PublishResult(String instagramPostId, LocalDateTime postedAt) {
	}

	// POST /api/posts/:id/publish — 즉시 업로드 (사용자 본인 소유 검증 포함)
	@Transactional
	public PublishResult publishNow(Long postId, Long userId) {
		Post post = postRepository.findByIdAndUserId(postId, userId)
				.orElseThrow(() -> ApiException.notFound("게시물을 찾을 수 없어요."));

		InstagramAccount account = instagramAccountRepository
				.findByIdAndUserId(post.getInstagramAccountId(), userId)
				.orElseThrow(() -> ApiException.notFound("연동된 인스타그램 계정을 찾을 수 없어요."));

		try {
			String instagramPostId = doPublish(post, account);
			post.setStatus(PostStatus.posted);
			post.setInstagramPostId(instagramPostId);
			post.setPostedAt(LocalDateTime.now());
			return new PublishResult(instagramPostId, post.getPostedAt());
		} catch (WebClientResponseException ex) {
			// Meta가 보낸 실제 에러 바디(코드/메시지)를 남긴다 — 기본 예외 메시지엔 body가 안 찍힘.
			log.error("[Instagram publish error] postId={} status={} body={}",
					postId, ex.getStatusCode(), ex.getResponseBodyAsString());
			post.setStatus(PostStatus.failed);
			throw ApiException.internal("인스타그램 업로드에 실패했어요. 다시 시도해주세요.");
		} catch (Exception ex) {
			log.error("[Instagram publish error] postId={}", postId, ex);
			post.setStatus(PostStatus.failed);
			throw ApiException.internal("인스타그램 업로드에 실패했어요. 다시 시도해주세요.");
		}
	}

	// 예약 발행(cron)용 — 소유권 검증 없이 post/account가 이미 확정된 상태에서 호출된다.
	public String publishForSchedule(Post post, InstagramAccount account) {
		return doPublish(post, account);
	}

	private String doPublish(Post post, InstagramAccount account) {
		String captionWithHashtags = buildCaptionWithHashtags(post);
		return metaApiClient.publishToInstagram(
				account.getInstagramUserId(), account.getAccessToken(), post.getImageUrl(), captionWithHashtags);
	}

	private String buildCaptionWithHashtags(Post post) {
		String hashtagLine = String.join(" ", HashtagUtil.toList(post.getHashtags()));
		if (post.getCaption() == null && hashtagLine.isBlank()) {
			return "";
		}
		if (post.getCaption() == null) {
			return hashtagLine;
		}
		if (hashtagLine.isBlank()) {
			return post.getCaption();
		}
		return post.getCaption() + "\n\n" + hashtagLine;
	}
}
