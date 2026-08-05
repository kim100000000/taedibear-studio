package com.taedibear.studio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

// S3 업로드 남용(대량 업로드로 인한 과금 폭탄) 방지용 일일 누적 사용량 카운터.
// 날짜 자체가 PK라서 자정이 지나면 새 행이 생기며 한도가 자동으로 리셋된다.
@Entity
@Table(name = "daily_upload_quota")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyUploadQuota {

	@Id
	@Column(name = "upload_date")
	private LocalDate uploadDate;

	@Column(name = "total_bytes", nullable = false)
	private long totalBytes;
}
