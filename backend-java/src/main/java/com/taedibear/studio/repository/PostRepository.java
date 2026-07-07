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

	// Phase 4-1: 계정별 히스토리 필터링 — accountId/status가 null이면 조건 무시
	@Query("SELECT p FROM Post p WHERE p.userId = :userId " +
		   "AND (:accountId IS NULL OR p.instagramAccountId = :accountId) " +
		   "AND (:status IS NULL OR p.status = :status)")
	Page<Post> findAllFiltered(@Param("userId") Long userId, @Param("accountId") Long accountId,
			@Param("status") PostStatus status, Pageable pageable);

	Optional<Post> findByIdAndUserId(Long id, Long userId);

	long countByUserId(Long userId);

	// Phase 4-1: 계정 필터를 적용한 게시물 총 개수 (analytics 예약 비율 분모용)
	@Query("SELECT COUNT(p) FROM Post p WHERE p.userId = :userId " +
		   "AND (:accountId IS NULL OR p.instagramAccountId = :accountId)")
	long countByUserIdFiltered(@Param("userId") Long userId, @Param("accountId") Long accountId);

	// ── Analytics 쿼리 ────────────────────────────────────────────────────────
	// Phase 4-1: 모든 analytics 쿼리에 accountId(nullable) 필터 추가 — null이면 전체 계정 합산

	/** 월별 업로드 수: [year, month, count] */
	@Query("SELECT YEAR(p.createdAt), MONTH(p.createdAt), COUNT(p) " +
		   "FROM Post p " +
		   "WHERE p.userId = :userId " +
		   "AND (:accountId IS NULL OR p.instagramAccountId = :accountId) " +
		   "AND p.createdAt >= FUNCTION('DATE_SUB', CURRENT_TIMESTAMP, 1, 'YEAR') " +
		   "GROUP BY YEAR(p.createdAt), MONTH(p.createdAt) " +
		   "ORDER BY YEAR(p.createdAt), MONTH(p.createdAt)")
	List<Object[]> findMonthlyUploadCounts(@Param("userId") Long userId, @Param("accountId") Long accountId);

	/** 상태별 게시물 수 (posted / failed) */
	@Query("SELECT COUNT(p) FROM Post p WHERE p.userId = :userId AND p.status = :status")
	long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") PostStatus status);

	/** 상태 이름(문자열)으로 게시물 수 조회 — PostStatus enum 변환 편의용 */
	@Query("SELECT COUNT(p) FROM Post p WHERE p.userId = :userId " +
		   "AND (:accountId IS NULL OR p.instagramAccountId = :accountId) " +
		   "AND LOWER(p.status) = LOWER(:statusName)")
	long countByUserIdAndStatusName(@Param("userId") Long userId, @Param("accountId") Long accountId,
			@Param("statusName") String statusName);

	/** 요일별 업로드 수: [dayOfWeek(1=일~7=토), count] — posted 상태 기준 */
	@Query("SELECT FUNCTION('DAYOFWEEK', p.postedAt), COUNT(p) " +
		   "FROM Post p " +
		   "WHERE p.userId = :userId AND p.status = com.taedibear.studio.domain.PostStatus.posted " +
		   "AND (:accountId IS NULL OR p.instagramAccountId = :accountId) " +
		   "AND p.postedAt IS NOT NULL " +
		   "GROUP BY FUNCTION('DAYOFWEEK', p.postedAt)")
	List<Object[]> findDailyPatternCounts(@Param("userId") Long userId, @Param("accountId") Long accountId);

	/** 이번 달 업로드 수 */
	@Query("SELECT COUNT(p) FROM Post p " +
		   "WHERE p.userId = :userId " +
		   "AND (:accountId IS NULL OR p.instagramAccountId = :accountId) " +
		   "AND YEAR(p.createdAt) = :year AND MONTH(p.createdAt) = :month")
	long countThisMonth(@Param("userId") Long userId, @Param("accountId") Long accountId,
			@Param("year") int year, @Param("month") int month);

	// Phase 2-3 관리자 리포트: 날짜 범위 내 posted 상태 건수 (재시도 성공 카운트용)
	long countByStatusAndPostedAtBetween(PostStatus status, LocalDateTime from, LocalDateTime to);

	// ── Phase 5-1 관리자 대시보드 ────────────────────────────────────────────
	/** 특정 시점 이후 생성된 게시물 수 (최근 7일/30일 지표) */
	long countByCreatedAtAfter(LocalDateTime since);

	/** 전체 상태별 게시물 수 (posted/failed 등 전 유저 합산) */
	long countByStatus(PostStatus status);

	/** 유저 목록 페이지에 표시할 유저별 게시물 수: [userId, count] */
	@Query("SELECT p.userId, COUNT(p) FROM Post p WHERE p.userId IN :userIds GROUP BY p.userId")
	List<Object[]> countGroupByUserIds(@Param("userIds") List<Long> userIds);

	// 회원 탈퇴: 사용자의 모든 게시물 삭제 (scheduled_posts 삭제 후, instagram_accounts 삭제 전)
	@Modifying
	@Query("delete from Post p where p.userId = :userId")
	void deleteAllByUserId(@Param("userId") Long userId);
}
