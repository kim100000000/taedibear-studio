package com.taedibear.studio.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// 개선백로그 🟡: 캡션 보관함 — 마음에 든 캡션을 저장해두고 새 게시물에서 불러온다.
// hashtags는 posts와 동일하게 쉼표 구분 TEXT (HashtagUtil).
@Entity
@Table(name = "saved_captions", indexes = {
		@Index(name = "idx_saved_captions_user_id", columnList = "user_id")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedCaption {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String caption;

	@Column(columnDefinition = "TEXT")
	private String hashtags;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;
}
