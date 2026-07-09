package com.taedibear.studio.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

// docs/04_DB설계서.md 3.1 users
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, unique = true, length = 255)
	private String email;

	@Column(name = "password_hash", length = 255)
	private String passwordHash;

	@Column(name = "google_id", length = 100)
	private String googleId;

	@Column(name = "kakao_id", length = 100)
	private String kakaoId;

	@Column(name = "naver_id", length = 100)
	private String naverId;

	// Phase 2-3: 업종/분위기 저장 → AI 캡션 생성 시 자동 반영
	@Column(name = "business_type", length = 50)
	private String businessType;

	@Column(name = "mood", length = 50)
	private String mood;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private Plan plan = Plan.free;

	// Phase 2-1: 크레딧 시스템 (보유 상한 10 — UserService.MAX_CREDITS)
	@Column(nullable = false)
	@Builder.Default
	// 가입 시 기본 3개 + 온보딩 완료 보너스 +2 = 총 5개 (CHECKLIST 크레딧 정책).
	// 버그 수정(2026-07-09): 기본값이 5로 잘못 들어가 온보딩 후 7개가 되던 문제 → 3으로 정정
	private int credits = 3;

	// 광고 시청 충전 — 하루 2회 제한
	@Column(name = "ad_watch_count")
	@Builder.Default
	private int adWatchCount = 0;

	@Column(name = "last_ad_watch_date")
	private LocalDate lastAdWatchDate;

	// 광고 시청 최소 간격 검증용 (마지막 광고 충전 시각)
	@Column(name = "last_ad_watch_at")
	private LocalDateTime lastAdWatchAt;

	// 온보딩 보너스 1회 지급 여부 — 반복 호출로 크레딧 무한 충전 방지
	@Column(name = "onboarding_completed", nullable = false)
	@Builder.Default
	private boolean onboardingCompleted = false;

	// Phase 6: 이메일 인증 여부 — 크레딧 다계정 어뷰징 방어 (미인증 시 캡션 생성 차단).
	// columnDefinition default 1: 컬럼 추가 마이그레이션 시 "기존 가입자"는 인증된 것으로 처리해
	// 잠금을 방지한다. 신규 이메일 가입자는 엔티티 기본값 false로 저장되고, 소셜 가입은
	// 제공자가 이메일을 이미 검증했으므로 가입 시 true로 저장한다 (AuthService).
	@Column(name = "email_verified", nullable = false, columnDefinition = "tinyint(1) default 1")
	@Builder.Default
	private boolean emailVerified = false;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;
}
