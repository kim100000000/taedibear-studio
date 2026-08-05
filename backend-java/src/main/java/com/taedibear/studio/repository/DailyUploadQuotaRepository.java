package com.taedibear.studio.repository;

import com.taedibear.studio.domain.DailyUploadQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface DailyUploadQuotaRepository extends JpaRepository<DailyUploadQuota, LocalDate> {

	// 오늘 누적치 + 이번 파일 크기가 한도 이하일 때만 원자적으로 증가시킨다(동시 요청 레이스 방지).
	// 영향받은 행이 0이면 (a) 오늘 행이 아직 없거나 (b) 한도 초과 — 호출부에서 구분해 처리한다.
	// clearAutomatically: JPQL bulk UPDATE는 영속성 컨텍스트(1차 캐시)를 거치지 않아,
	// 같은 트랜잭션 안에서 이 메서드 이후 같은 엔티티를 재조회하면 stale 값이 나올 수 있다 — 자동으로 캐시를 비워 방지.
	@Modifying(clearAutomatically = true)
	@Query("UPDATE DailyUploadQuota q SET q.totalBytes = q.totalBytes + :bytes " +
		   "WHERE q.uploadDate = :date AND q.totalBytes + :bytes <= :limitBytes")
	int tryReserve(@Param("date") LocalDate date, @Param("bytes") long bytes, @Param("limitBytes") long limitBytes);
}
