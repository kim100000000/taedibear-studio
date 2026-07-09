package com.taedibear.studio.repository;

import com.taedibear.studio.domain.ReviewComment;
import com.taedibear.studio.domain.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewCommentRepository extends JpaRepository<ReviewComment, Long> {

	Optional<ReviewComment> findByCommentId(String commentId);

	List<ReviewComment> findAllByInstagramAccountIdOrderByCreatedAtDesc(Long instagramAccountId);

	List<ReviewComment> findAllByInstagramAccountIdAndStatus(Long instagramAccountId, ReviewStatus status);

	// 개선백로그 🔴: 인스타에서 삭제된 댓글 정리용 — 게시물 단위로 로컬 댓글 조회
	List<ReviewComment> findAllByMediaId(String mediaId);
}
