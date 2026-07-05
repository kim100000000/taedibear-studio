package com.taedibear.studio.repository;

import com.taedibear.studio.domain.Post;
import com.taedibear.studio.domain.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
	Page<Post> findAllByUserId(Long userId, Pageable pageable);

	Page<Post> findAllByUserIdAndStatus(Long userId, PostStatus status, Pageable pageable);

	Optional<Post> findByIdAndUserId(Long id, Long userId);

	long countByUserId(Long userId);

	// ── Analytics 쿼리 ────────────────────────────────────────────────────────

	/** 월별 업로드 수: [year, month, count] */
	@Query("SELECT YEAR(p.createdAt), MONTH(p.createdAt), COUNT(p) " +
		   "FROM Post p " +
		   "WHERE p.userId = :userId " +
		   "AND p.createdAt >= FUNCTION('DATE_SUB', CURRENT_TIMESTAMP, 1, 'YEAR') " +
		   "GROUP BY YEAR(p.createdAt), MONTH(p.createdAt) " +
		   "ORDER BY YEAR(p.createdAt), MONTH(p.createdAt)")
	List<Object[]> findMonthlyUploadCounts(@Param("userId") Long userId);

	/** 상태별 게시물 수 (posted / failed) */
	@Query("SELECT COUNT(p) FROM Post p WHERE p.userId = :userId AND p.status = :status")
	long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") PostStatus status);

	/** 상태 이름(문자열)으로 게시물 수 조회 — PostStatus enum 변환 편의용 */
	@Query("SELECT COUNT(p) FROM Post p WHERE p.userId = :userId AND LOWER(p.status) = LOWER(:statusName)")
	long countByUserIdAndStatusName(@Param("userId") Long userId, @Param("statusName") String statusName);

	/** 요일별 업로드 수: [dayOfWeek(1=일~7=토), count] — posted 상태 기준 */
	@Query("SELECT FUNCTION('DAYOFWEEK', p.postedAt), COUNT(p) " +
		   "FROM Post p " +
		   "WHERE p.userId = :userId AND p.status = com.taedibear.studio.domain.PostStatus.posted " +
		   "AND p.postedAt IS NOT NULL " +
		   "GROUP BY FUNCTION('DAYOFWEEK', p.postedAt)")
	List<Object[]> findDailyPatternCounts(@Param("userId") Long userId);

	/** 이번 달 업로드 수 */
	@Query("SELECT COUNT(p) FROM Post p " +
		   "WHERE p.userId = :userId " +
		   "AND YEAR(p.createdAt) = :year AND MONTH(p.createdAt) = :month")
	long countThisMonth(@Param("userId") Long userId, @Param("year") int year, @Param("month") int month);

	// Phase 2-3 관리자 리포트: 날짜 범위 내 posted 상태 건수 (재시도 성공 카운트용)
	long countByStatusAndPostedAtBetween(PostStatus status, LocalDateTime from, LocalDateTime to);

	// 회원 탈퇴: 사용자의 모든 게시물 삭제 (scheduled_posts 삭제 후, instagram_accounts 삭제 전)
	@Modifying
	@Query("delete from Post p where p.userId = :userId")
	void deleteAllByUserId(@Param("userId") Long userId);
}
