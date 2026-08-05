package com.taedibear.studio.post;

import com.taedibear.studio.common.exception.ApiException;
import com.taedibear.studio.domain.DailyUploadQuota;
import com.taedibear.studio.repository.DailyUploadQuotaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

// 계정 탈취/봇에 의한 대량 업로드로 S3 비용이 폭주하는 걸 막는 최후 안전장치.
// rate limit(요청 빈도 제한)이 아직 없어서, 그 사이 임시로 하루 총 업로드 용량 자체를 제한한다.
@Service
public class UploadQuotaService {

	// 사진 1장이 보통 몇 MB인 걸 감안하면 정상적인 하루 사용량으로 충분하고,
	// 공격으로 인한 대량 업로드는 여기서 바로 막힌다.
	private static final long DAILY_LIMIT_BYTES = 100L * 1024 * 1024; // 100MB/일

	private final DailyUploadQuotaRepository repository;

	public UploadQuotaService(DailyUploadQuotaRepository repository) {
		this.repository = repository;
	}

	@Transactional
	public void reserve(long fileSizeBytes) {
		LocalDate today = LocalDate.now();

		if (repository.tryReserve(today, fileSizeBytes, DAILY_LIMIT_BYTES) > 0) {
			return;
		}

		// 오늘 행이 아직 없을 수 있으니 만들고 한 번 더 시도한다.
		if (!repository.existsById(today)) {
			try {
				repository.save(DailyUploadQuota.builder().uploadDate(today).totalBytes(0).build());
			} catch (DataIntegrityViolationException e) {
				// 동시 요청이 먼저 오늘 행을 만든 경우 — 무시하고 아래에서 재시도한다.
			}
		}

		if (repository.tryReserve(today, fileSizeBytes, DAILY_LIMIT_BYTES) == 0) {
			throw ApiException.tooManyRequests("오늘 업로드 가능한 용량(100MB)을 모두 사용했어요. 내일 다시 시도해주세요.");
		}
	}
}
