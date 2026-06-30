package com.taedibear.studio.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// docs/04_DB설계서.md 3.3 posts
// hashtags는 Node 버전과 동일하게 TEXT 컬럼에 쉼표(,) 구분 문자열로 저장한다.
@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "instagram_account_id", nullable = false)
	private Long instagramAccountId;

	@Lob
	@Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
	private String imageUrl;

	@Lob
	@Column(columnDefinition = "TEXT")
	private String caption;

	@Lob
	@Column(columnDefinition = "TEXT")
	private String hashtags;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private PostStatus status = PostStatus.draft;

	@Column(name = "instagram_post_id", length = 100)
	private String instagramPostId;

	@Column(name = "posted_at")
	private LocalDateTime postedAt;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;
}
