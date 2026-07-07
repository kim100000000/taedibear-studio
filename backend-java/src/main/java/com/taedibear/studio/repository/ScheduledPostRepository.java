package com.taedibear.studio.repository;

import com.taedibear.studio.domain.ScheduledPost;
import com.taedibear.studio.domain.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ScheduledPostRepository extends JpaRepository<ScheduledPost, Long> {

	Optional<ScheduledPost> findByPostId(Long postId);

	// docs/04_DB설계서.md 5. 예약된 게시물 조회 (cron job용)와 동일한 조건
	List<ScheduledPost> findAllByStatusAndScheduledAtLessThanEqualAndRetryCountLessThan(
			ScheduleStatus status, LocalDateTime now, Integer maxRetryCount);

	// 사용자 소유 게시물에 연결된 예약만 조회 (GET /api/scheduled)
	@Query("select sp from ScheduledPost sp where sp.postId in " +
			"(select p.id from Post p where p.userId = :userId) order by sp.scheduledAt asc")
	List<ScheduledPost> findAllByPostUserId(@Param("userId") Long userId);

	// 본인 게시물에 연결된 예약 1건 (PUT/DELETE /api/scheduled/:id 소유권 검증용)
	@Query("select sp from ScheduledPost sp where sp.id = :id and sp.postId in " +
			"(select p.id from Post p where p.userId = :userId)")
	Optional<ScheduledPost> findByIdAndPostUserId(@Param("id") Long id, @Param("userId") Long userId);

	// Analytics: 사용자의 게시물 중 예약이 걸렸던 post 수 (중복 제거)
	// Phase 4-1: accountId(nullable) 필터 추가 — null이면 전체 계정 합산
	@Query("select count(distinct sp.postId) from ScheduledPost sp where sp.postId in " +
			"(select p.id from Post p where p.userId = :userId " +
			"and (:accountId is null or p.instagramAccountId = :accountId))")
	long countScheduledPostsByUserId(@Param("userId") Long userId, @Param("accountId") Long accountId);

	// Phase 2-3 관리자 리포트: 날짜 범위 내 상태별 건수
	long countByStatusAndScheduledAtBetween(ScheduleStatus status, LocalDateTime from, LocalDateTime to);

	// Phase 5-1 관리자 대시보드: 전체 상태별 예약 건수 (대기/실패 현황)
	long countByStatus(ScheduleStatus status);

	// 회원 탈퇴: 사용자 소유 게시물에 연결된 예약 전체 삭제 (posts보다 먼저 삭제되어야 FK 위반 방지)
	@Modifying
	@Query("delete from ScheduledPost sp where sp.postId in " +
			"(select p.id from Post p where p.userId = :userId)")
	void deleteAllByPostUserId(@Param("userId") Long userId);
}
