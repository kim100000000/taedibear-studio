package com.taedibear.studio.repository;

import com.taedibear.studio.domain.FollowerSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FollowerSnapshotRepository extends JpaRepository<FollowerSnapshot, Long> {

	Optional<FollowerSnapshot> findByInstagramAccountIdAndSnapshotDate(Long instagramAccountId, LocalDate snapshotDate);

	// Phase 4-2: 계정이 지정되면 해당 계정만, 없으면 사용자 소유 전체 계정을 합산해 날짜별로 반환
	@Query("SELECT fs.snapshotDate, SUM(fs.followersCount) FROM FollowerSnapshot fs " +
		   "WHERE fs.instagramAccountId IN " +
		   "(SELECT ia.id FROM InstagramAccount ia WHERE ia.userId = :userId " +
		   "AND (:accountId IS NULL OR ia.id = :accountId)) " +
		   "AND fs.snapshotDate >= :from " +
		   "GROUP BY fs.snapshotDate ORDER BY fs.snapshotDate")
	List<Object[]> findTrend(@Param("userId") Long userId, @Param("accountId") Long accountId,
			@Param("from") LocalDate from);
}
