package com.taedibear.studio.repository;

import com.taedibear.studio.domain.DailyUploadQuota;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

// 실 로컬 MySQL(dev DB)에 붙어서 tryReserve JPQL의 "한도 이하일 때만 원자적으로 증가" 동작을 검증한다.
// @DataJpaTest는 기본적으로 트랜잭션을 각 테스트 후 롤백하므로 dev DB에 데이터가 남지 않는다.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=update")
class DailyUploadQuotaRepositoryTest {

	@Autowired
	private DailyUploadQuotaRepository repository;

	private static final LocalDate TEST_DATE = LocalDate.of(2999, 1, 1); // 실 데이터와 절대 안 겹치게 미래 날짜 사용

	@Test
	void 한도_이내면_증가하고_영향행수_1을_반환한다() {
		repository.save(DailyUploadQuota.builder().uploadDate(TEST_DATE).totalBytes(90L * 1024 * 1024).build());

		int updated = repository.tryReserve(TEST_DATE, 5L * 1024 * 1024, 100L * 1024 * 1024);

		assertThat(updated).isEqualTo(1);
		assertThat(repository.findById(TEST_DATE).orElseThrow().getTotalBytes()).isEqualTo(95L * 1024 * 1024);
	}

	@Test
	void 한도_초과면_증가하지_않고_영향행수_0을_반환한다() {
		repository.save(DailyUploadQuota.builder().uploadDate(TEST_DATE).totalBytes(98L * 1024 * 1024).build());

		int updated = repository.tryReserve(TEST_DATE, 5L * 1024 * 1024, 100L * 1024 * 1024); // 98+5 > 100

		assertThat(updated).isEqualTo(0);
		assertThat(repository.findById(TEST_DATE).orElseThrow().getTotalBytes()).isEqualTo(98L * 1024 * 1024); // 변화 없음
	}

	@Test
	void 정확히_한도에_딱_맞으면_통과한다() {
		repository.save(DailyUploadQuota.builder().uploadDate(TEST_DATE).totalBytes(95L * 1024 * 1024).build());

		int updated = repository.tryReserve(TEST_DATE, 5L * 1024 * 1024, 100L * 1024 * 1024); // 95+5 == 100

		assertThat(updated).isEqualTo(1);
	}

	@Test
	void 오늘_행이_없으면_영향행수_0을_반환한다() {
		int updated = repository.tryReserve(TEST_DATE, 1024L, 100L * 1024 * 1024);

		assertThat(updated).isEqualTo(0);
		assertThat(repository.existsById(TEST_DATE)).isFalse();
	}
}
