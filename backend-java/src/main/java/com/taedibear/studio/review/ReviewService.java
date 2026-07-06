package com.taedibear.studio.review;

import com.taedibear.studio.common.exception.ApiException;
import com.taedibear.studio.domain.InstagramAccount;
import com.taedibear.studio.domain.ReviewComment;
import com.taedibear.studio.domain.ReviewStatus;
import com.taedibear.studio.domain.User;
import com.taedibear.studio.instagram.MetaApiClient;
import com.taedibear.studio.post.GeminiService;
import com.taedibear.studio.repository.InstagramAccountRepository;
import com.taedibear.studio.repository.ReviewCommentRepository;
import com.taedibear.studio.repository.UserRepository;
import com.taedibear.studio.review.dto.CommentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Phase 4-3: 리뷰(댓글) 자동 답글.
 * Meta에는 댓글의 "이미 답글 달았는지" 상태를 매번 물어보는 대신, 로컬 review_comments 테이블에
 * comment_id를 유니크 키로 저장해 동기화 시 중복 생성/중복 답글을 막는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

	private static final int RECENT_MEDIA_LIMIT = 5;

	private final InstagramAccountRepository instagramAccountRepository;
	private final ReviewCommentRepository reviewCommentRepository;
	private final UserRepository userRepository;
	private final MetaApiClient metaApiClient;
	private final GeminiService geminiService;

	/** 최근 게시물의 댓글을 Meta에서 가져와 로컬에 동기화(신규만 삽입 + AI 답글 초안 생성) 후 목록을 반환 */
	@Transactional
	public List<CommentResponse> listComments(Long accountId, Long userId) {
		InstagramAccount account = getOwnedAccount(accountId, userId);
		syncComments(account, userId);
		return reviewCommentRepository.findAllByInstagramAccountIdOrderByCreatedAtDesc(accountId).stream()
				.map(this::toResponse)
				.toList();
	}

	private void syncComments(InstagramAccount account, Long userId) {
		String businessType = userRepository.findById(userId).map(User::getBusinessType).orElse(null);

		List<MetaApiClient.MediaSummary> mediaList;
		try {
			mediaList = metaApiClient.getRecentMedia(account.getInstagramUserId(), account.getAccessToken(), RECENT_MEDIA_LIMIT);
		} catch (Exception ex) {
			log.warn("[리뷰 동기화] 최근 게시물 조회 실패 accountId={} — {}", account.getId(), ex.getMessage());
			return;
		}

		for (MetaApiClient.MediaSummary media : mediaList) {
			List<MetaApiClient.Comment> comments;
			try {
				comments = metaApiClient.getComments(media.id(), account.getAccessToken());
			} catch (Exception ex) {
				log.warn("[리뷰 동기화] 댓글 조회 실패 mediaId={} — {}", media.id(), ex.getMessage());
				continue;
			}

			for (MetaApiClient.Comment comment : comments) {
				if (reviewCommentRepository.findByCommentId(comment.id()).isPresent()) {
					continue; // 이미 동기화된 댓글 — 건너뜀 (중복 방지)
				}

				String suggestedReply = null;
				try {
					suggestedReply = geminiService.generateCommentReply(comment.text(), businessType);
				} catch (Exception ex) {
					log.warn("[리뷰 동기화] AI 답글 생성 실패 commentId={} — {}", comment.id(), ex.getMessage());
				}

				ReviewComment entity = ReviewComment.builder()
						.instagramAccountId(account.getId())
						.mediaId(media.id())
						.commentId(comment.id())
						.commentText(comment.text())
						.username(comment.username())
						.suggestedReply(suggestedReply)
						.status(ReviewStatus.pending)
						.build();
				reviewCommentRepository.save(entity);
			}
		}
	}

	/** 사용자가 승인한 답글(또는 수정한 내용)을 실제로 Meta에 발행 */
	@Transactional
	public void reply(Long reviewCommentId, Long userId, String message) {
		ReviewComment comment = reviewCommentRepository.findById(reviewCommentId)
				.orElseThrow(() -> ApiException.notFound("댓글을 찾을 수 없어요."));
		InstagramAccount account = getOwnedAccount(comment.getInstagramAccountId(), userId);

		if (comment.getStatus() == ReviewStatus.replied) {
			throw ApiException.badRequest("이미 답글을 단 댓글이에요.");
		}

		String postedReplyId = metaApiClient.replyToComment(comment.getCommentId(), account.getAccessToken(), message);
		comment.setStatus(ReviewStatus.replied);
		comment.setPostedReplyId(postedReplyId);
		comment.setSuggestedReply(message);
	}

	/** 이 댓글은 답글을 달지 않고 넘어간다 */
	@Transactional
	public void skip(Long reviewCommentId, Long userId) {
		ReviewComment comment = reviewCommentRepository.findById(reviewCommentId)
				.orElseThrow(() -> ApiException.notFound("댓글을 찾을 수 없어요."));
		getOwnedAccount(comment.getInstagramAccountId(), userId); // 소유권 검증
		comment.setStatus(ReviewStatus.skipped);
	}

	// Phase 4-3 자동 발행 스케줄러 전용 — 사용자 인증 컨텍스트 없이 계정 단위로 동기화 + 자동 답글
	@Transactional
	public void syncAndAutoReply(InstagramAccount account) {
		syncComments(account, account.getUserId());
		List<ReviewComment> pending = reviewCommentRepository
				.findAllByInstagramAccountIdAndStatus(account.getId(), ReviewStatus.pending);
		for (ReviewComment comment : pending) {
			if (comment.getSuggestedReply() == null || comment.getSuggestedReply().isBlank()) continue;
			try {
				String postedReplyId = metaApiClient.replyToComment(
						comment.getCommentId(), account.getAccessToken(), comment.getSuggestedReply());
				comment.setStatus(ReviewStatus.replied);
				comment.setPostedReplyId(postedReplyId);
			} catch (Exception ex) {
				log.warn("[자동 답글 실패] commentId={} — {}", comment.getCommentId(), ex.getMessage());
			}
		}
	}

	private InstagramAccount getOwnedAccount(Long accountId, Long userId) {
		return instagramAccountRepository.findByIdAndUserId(accountId, userId)
				.orElseThrow(() -> ApiException.notFound("연동된 인스타그램 계정을 찾을 수 없어요."));
	}

	private CommentResponse toResponse(ReviewComment c) {
		return new CommentResponse(c.getId(), c.getMediaId(), c.getCommentText(), c.getUsername(),
				c.getSuggestedReply(), c.getStatus().name(), c.getCreatedAt());
	}
}
