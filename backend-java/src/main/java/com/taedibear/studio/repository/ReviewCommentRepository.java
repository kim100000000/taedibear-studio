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
}
