package com.taedibear.studio.post;

import com.taedibear.studio.common.exception.ApiException;
import com.taedibear.studio.domain.DailyUploadQuota;
import com.taedibear.studio.repository.DailyUploadQuotaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadQuotaServiceTest {

	@Mock
	private DailyUploadQuotaRepository repository;

	private UploadQuotaService uploadQuotaService;

	private UploadQuotaService service() {
		return new UploadQuotaService(repository);
	}

	@Test
	void 한도_이내면_바로_통과한다() {
		uploadQuotaService = service();
		when(repository.tryReserve(any(), anyLong(), anyLong())).thenReturn(1);

		uploadQuotaService.reserve(5L * 1024 * 1024); // 5MB

		verify(repository, never()).existsById(any());
		verify(repository, never()).save(any());
	}

	@Test
	void 오늘_행이_없으면_생성하고_재시도해서_통과한다() {
		uploadQuotaService = service();
		LocalDate today = LocalDate.now();
		when(repository.tryReserve(any(), anyLong(), anyLong()))
				.thenReturn(0)  // 첫 시도: 행 없음
				.thenReturn(1); // 재시도: 통과
		when(repository.existsById(today)).thenReturn(false);

		uploadQuotaService.reserve(1024L);

		ArgumentCaptor<DailyUploadQuota> captor = ArgumentCaptor.forClass(DailyUploadQuota.class);
		verify(repository).save(captor.capture());
		assertThat(captor.getValue().getUploadDate()).isEqualTo(today);
		assertThat(captor.getValue().getTotalBytes()).isZero();
	}

	@Test
	void 한도_초과면_429를_던지고_S3에_닿지_않는다() {
		uploadQuotaService = service();
		LocalDate today = LocalDate.now();
		when(repository.tryReserve(any(), anyLong(), anyLong())).thenReturn(0); // 매번 초과
		when(repository.existsById(today)).thenReturn(true); // 오늘 행은 이미 존재 (한도만 초과)

		assertThatThrownBy(() -> uploadQuotaService.reserve(200L * 1024 * 1024))
				.isInstanceOf(ApiException.class)
				.satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));

		verify(repository, never()).save(any());
	}

	@Test
	void 동시요청이_먼저_행을_만들어도_재시도로_정상_처리된다() {
		uploadQuotaService = service();
		LocalDate today = LocalDate.now();
		when(repository.tryReserve(any(), anyLong(), anyLong()))
				.thenReturn(0)
				.thenReturn(1);
		when(repository.existsById(today)).thenReturn(true); // 다른 스레드가 이미 생성함

		uploadQuotaService.reserve(1024L);

		verify(repository, never()).save(any());
	}
}
