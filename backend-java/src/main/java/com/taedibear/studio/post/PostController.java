package com.taedibear.studio.post;

import com.taedibear.studio.common.ApiResponse;
import com.taedibear.studio.common.exception.ApiException;
import com.taedibear.studio.domain.Post;
import com.taedibear.studio.instagram.InstagramPublishService;
import com.taedibear.studio.post.dto.*;
import com.taedibear.studio.security.UserPrincipal;
import com.taedibear.studio.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

	private final PostService postService;
	private final S3Service s3Service;
	private final GeminiService geminiService;
	private final InstagramPublishService instagramPublishService;
	private final UserService userService;

	// POST /api/posts/upload
	@PostMapping(value = "/upload", consumes = "multipart/form-data")
	public ApiResponse<Map<String, String>> uploadImage(@RequestParam("image") MultipartFile image) {
		if (image == null || image.isEmpty()) {
			throw ApiException.badRequest("이미지 파일이 필요해요.");
		}
		String imageUrl = s3Service.uploadImage(image);
		return ApiResponse.ok(Map.of("image_url", imageUrl));
	}

	// POST /api/posts/caption — Phase 2-3: 선택 필드 추가, 업종/분위기 저장
	@PostMapping("/caption")
	public ApiResponse<CaptionResponse> generateCaption(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestBody CaptionRequest request) {
		if (request.image_url() == null || request.business_type() == null || request.mood() == null) {
			throw ApiException.badRequest("image_url, business_type, mood는 필수예요.");
		}
		// Phase 6: 이메일 인증 게이트 — 미인증 계정은 캡션 생성(크레딧 사용) 불가 (다계정 어뷰징 방어)
		userService.assertEmailVerified(principal.getId());
		// Phase 2-1: 크레딧 차감 (Free 플랜만, 부족하면 403)
		userService.deductCreditForCaption(principal.getId());
		// 업종/분위기 선택값을 users 테이블에 저장 → 다음 방문 시 자동 반영
		userService.updateBusinessProfile(principal.getId(), request.business_type(), request.mood());

		var result = geminiService.generateCaption(
				request.image_url(), request.business_type(), request.mood(),
				request.special_menu(), request.event_promotion(), request.keywords());
		return ApiResponse.ok(new CaptionResponse(result.caption(), result.hashtags()));
	}

	// POST /api/posts — draft 상태로 저장
	@PostMapping
	public org.springframework.http.ResponseEntity<ApiResponse<Map<String, Object>>> createPost(
			@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody CreatePostRequest request) {
		Post post = postService.createPost(principal.getId(), request);
		var body = ApiResponse.ok(Map.<String, Object>of(
				"id", post.getId(), "status", post.getStatus().name(), "created_at", post.getCreatedAt()));
		return org.springframework.http.ResponseEntity.status(201).body(body);
	}

	// GET /api/posts — 히스토리 조회 (Phase 4-1: instagram_account_id로 계정별 필터링)
	@GetMapping
	public ApiResponse<PostListResponse> listPosts(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) Long instagram_account_id,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int limit) {
		return ApiResponse.ok(postService.listPosts(principal.getId(), status, instagram_account_id, page, limit));
	}

	// GET /api/posts/:id
	@GetMapping("/{id}")
	public ApiResponse<PostDetailResponse> getPost(
			@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
		return ApiResponse.ok(postService.getPostDetail(id, principal.getId()));
	}

	// PUT /api/posts/:id
	@PutMapping("/{id}")
	public ApiResponse<Map<String, Object>> updatePost(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long id,
			@RequestBody UpdatePostRequest request) {
		Post post = postService.updatePost(id, principal.getId(), request);
		return ApiResponse.ok(Map.of("id", post.getId()));
	}

	// DELETE /api/posts/:id
	@DeleteMapping("/{id}")
	public Map<String, Boolean> deletePost(
			@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
		postService.deletePost(id, principal.getId());
		return Map.of("success", true);
	}

	// POST /api/posts/:id/publish — 즉시 인스타그램 업로드
	@PostMapping("/{id}/publish")
	public ApiResponse<Map<String, Object>> publishPost(
			@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
		var result = instagramPublishService.publishNow(id, principal.getId());
		return ApiResponse.ok(Map.of("instagram_post_id", result.instagramPostId(), "posted_at", result.postedAt()));
	}

	// GET /api/posts/:id/insights — Phase 4-2: 게시물 조회수/좋아요/저장 등 인사이트
	@GetMapping("/{id}/insights")
	public ApiResponse<PostInsightsResponse> getPostInsights(
			@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
		return ApiResponse.ok(postService.getPostInsights(id, principal.getId()));
	}
}
