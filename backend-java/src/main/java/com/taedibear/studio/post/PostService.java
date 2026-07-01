package com.taedibear.studio.post;

import com.taedibear.studio.common.exception.ApiException;
import com.taedibear.studio.domain.Post;
import com.taedibear.studio.domain.PostStatus;
import com.taedibear.studio.domain.ScheduledPost;
import com.taedibear.studio.payment.PaymentService;
import com.taedibear.studio.post.dto.*;
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
		Pageable pageable = PageRequest.of(Math.max(page - 1, 0), limit, Sort.by("createdAt").descending());

		Page<Post> result = (status != null && !status.isBlank())
				? postRepository.findAllByUserIdAndStatus(userId, PostStatus.valueOf(status), pageable)
				: postRepository.findAllByUserId(userId, pageable);

		var items = result.getContent().stream()
				.map(post -> {
					var scheduledAt = scheduledPostRepository.findByPostId(post.getId())
							.map(ScheduledPost::getScheduledAt)
							.orElse(null);
					return new PostListItemResponse(
							post.getId(), post.getImageUrl(), post.getCaption(),
							post.getStatus().name(), post.getPostedAt(), scheduledAt);
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
}
