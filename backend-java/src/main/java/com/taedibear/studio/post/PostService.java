package com.taedibear.studio.post;

import com.taedibear.studio.common.exception.ApiException;
import com.taedibear.studio.domain.InstagramAccount;
import com.taedibear.studio.domain.Post;
import com.taedibear.studio.domain.PostStatus;
import com.taedibear.studio.domain.ScheduledPost;
import com.taedibear.studio.instagram.MetaApiClient;
import com.taedibear.studio.payment.PaymentService;
import com.taedibear.studio.post.dto.*;
import com.taedibear.studio.repository.InstagramAccountRepository;
import com.taedibear.studio.repository.PostRepository;
import com.taedibear.studio.repository.ScheduledPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

	private final PostRepository postRepository;
	private final ScheduledPostRepository scheduledPostRepository;
	private final PaymentService paymentService;
	private final InstagramAccountRepository instagramAccountRepository;
	private final MetaApiClient metaApiClient;

	@Transactional
	public Post createPost(Long userId, CreatePostRequest request) {
		// Free 플랜 월 10회 제한 체크
		paymentService.checkPlanLimit(userId);

		Post post = Post.builder()
				.userId(userId)
				.instagramAccountId(request.instagram_account_id())
				.imageUrl(request.image_url())
				.caption(request.caption())
				.hashtags(HashtagUtil.toStorageString(request.hashtags()))
				.status(PostStatus.draft)
				.build();
		return postRepository.save(post);
	}

	public PostListResponse listPosts(Long userId, String status, int page, int limit) {
		return listPosts(userId, status, null, page, limit);
	}

	// Phase 4-1: instagramAccountId(nullable)로 계정별 히스토리 필터링
	public PostListResponse listPosts(Long userId, String status, Long instagramAccountId, int page, int limit) {
		Pageable pageable = PageRequest.of(Math.max(page - 1, 0), limit, Sort.by("createdAt").descending());
		PostStatus statusEnum = (status != null && !status.isBlank()) ? PostStatus.valueOf(status) : null;

		Page<Post> result = postRepository.findAllFiltered(userId, instagramAccountId, statusEnum, pageable);

		var items = result.getContent().stream()
				.map(post -> {
					var scheduledAt = scheduledPostRepository.findByPostId(post.getId())
							.map(ScheduledPost::getScheduledAt)
							.orElse(null);
					return new PostListItemResponse(
							post.getId(), post.getInstagramAccountId(), post.getImageUrl(), post.getCaption(),
							post.getStatus().name(), post.getInstagramPostId(), post.getPostedAt(), scheduledAt);
				})
				.toList();

		return new PostListResponse(items, result.getTotalElements(), page, limit);
	}

	public Post getOwnedPost(Long postId, Long userId) {
		return postRepository.findByIdAndUserId(postId, userId)
				.orElseThrow(() -> ApiException.notFound("게시물을 찾을 수 없어요."));
	}

	public PostDetailResponse getPostDetail(Long postId, Long userId) {
		Post post = getOwnedPost(postId, userId);
		return new PostDetailResponse(
				post.getId(), post.getImageUrl(), post.getCaption(),
				HashtagUtil.toList(post.getHashtags()), post.getStatus().name(),
				post.getInstagramPostId(), post.getPostedAt());
	}

	@Transactional
	public Post updatePost(Long postId, Long userId, UpdatePostRequest request) {
		Post post = getOwnedPost(postId, userId);
		if (post.getStatus() == PostStatus.posted) {
			throw ApiException.badRequest("업로드 완료된 게시물은 수정할 수 없어요.");
		}
		if (request.caption() != null) {
			post.setCaption(request.caption());
		}
		if (request.hashtags() != null) {
			post.setHashtags(HashtagUtil.toStorageString(request.hashtags()));
		}
		return post;
	}

	@Transactional
	public void deletePost(Long postId, Long userId) {
		Post post = getOwnedPost(postId, userId);
		postRepository.delete(post);
	}

	// Phase 4-2: 게시물별 인사이트(조회수/도달/좋아요/저장 등) 조회 — 발행된 게시물만 가능
	public PostInsightsResponse getPostInsights(Long postId, Long userId) {
		Post post = getOwnedPost(postId, userId);
		if (post.getStatus() != PostStatus.posted || post.getInstagramPostId() == null) {
			throw ApiException.badRequest("인스타그램에 업로드된 게시물만 인사이트를 볼 수 있어요.");
		}
		InstagramAccount account = instagramAccountRepository
				.findByIdAndUserId(post.getInstagramAccountId(), userId)
				.orElseThrow(() -> ApiException.notFound("연동된 인스타그램 계정을 찾을 수 없어요."));

		MetaApiClient.MediaInsights insights;
		try {
			insights = metaApiClient.getMediaInsights(post.getInstagramPostId(), account.getAccessToken());
		} catch (Exception ex) {
			throw ApiException.internal(
					"인사이트를 불러오지 못했어요. 계정에 instagram_manage_insights 권한이 있는지 확인하고, " +
					"권한을 새로 추가했다면 설정에서 인스타그램 계정을 다시 연동해주세요.");
		}
		return new PostInsightsResponse(insights.engagement(), insights.impressions(), insights.reach(),
				insights.likeCount(), insights.commentsCount());
	}
}
