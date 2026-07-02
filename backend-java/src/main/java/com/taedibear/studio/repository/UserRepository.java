package com.taedibear.studio.repository;

import com.taedibear.studio.domain.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByEmail(String email);

	Optional<User> findByGoogleId(String googleId);

	Optional<User> findByKakaoId(String kakaoId);

	Optional<User> findByNaverId(String naverId);

	boolean existsByEmail(String email);

	/**
	 * SELECT ... FOR UPDATE — 해당 사용자 행에 쓰기 잠금을 걸고 조회한다.
	 * 크레딧 지급/차감처럼 "읽고 → 검사하고 → 쓰는" 로직에서 동시 요청이
	 * 끼어들지 못하게 직렬화하는 용도 (트랜잭션 안에서만 사용 가능).
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from User u where u.id = :id")
	Optional<User> findByIdForUpdate(@Param("id") Long id);

	/**
	 * 크레딧 1개 원자적 차감. credits > 0 조건이 UPDATE 문 자체에 포함되어
	 * DB가 검사와 차감을 한 번에 처리하므로 동시 요청으로 음수가 될 수 없다.
	 * @return 차감 성공 시 1, 크레딧 부족 시 0
	 */
	@Modifying(clearAutomatically = true)
	@Query("update User u set u.credits = u.credits - 1 where u.id = :id and u.credits > 0")
	int deductOneCredit(@Param("id") Long id);
}
