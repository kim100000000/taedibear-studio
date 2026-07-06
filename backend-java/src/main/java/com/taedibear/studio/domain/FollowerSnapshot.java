package com.taedibear.studio.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

// Phase 4-2: 팔로워 증가 추이 — 계정별로 하루 1회 스냅샷을 저장한다.
// Instagram Insights API는 과거 팔로워 수 추이를 직접 제공하지 않으므로, 매일 현재 값을 기록해 누적한다.
@Entity
@Table(name = "follower_snapshots", uniqueConstraints = {
		@UniqueConstraint(columnNames = {"instagram_account_id", "snapshot_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowerSnapshot {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "instagram_account_id", nullable = false)
	private Long instagramAccountId;

	@Column(name = "followers_count", nullable = false)
	private long followersCount;

	@Column(name = "snapshot_date", nullable = false)
	private LocalDate snapshotDate;
}
