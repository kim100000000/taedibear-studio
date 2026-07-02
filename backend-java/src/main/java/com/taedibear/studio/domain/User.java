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

	// Phase 2-1: 크레딧 시스템
	@Column(nullable = false)
	@Builder.Default
	private int credits = 3;  // 가입 시 기본 3개

	// 광고 시청 충전 — 하루 2회 제한
	@Column(name = "ad_watch_count")
	@Builder.Default
	private int adWatchCount = 0;

	@Column(name = "last_ad_watch_date")
	private LocalDate lastAdWatchDate;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;
}
