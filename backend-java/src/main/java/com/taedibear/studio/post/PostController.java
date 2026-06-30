package com.taedibear.studio.post;

import com.taedibear.studio.common.ApiResponse;
import com.taedibear.studio.common.exception.ApiException;
import com.taedibear.studio.domain.Post;
import com.taedibear.studio.instagram.InstagramPublishService;
import com.taedibear.studio.post.dto.*;
import com.taedibear.studio.security.UserPrincipal;
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

	// POST /api/posts/upload
	@PostMapping(value = "/upload", consumes = "multipart/form-data")
	public ApiResponse<Map<String, String>> uploadImage(@RequestParam("image") MultipartFile image) {
		if (image == null || image.isEmpty()) {
			throw ApiException.badRequest("이미지 파일이 필요해요.");
		}
		String imageUrl = s3Service.uploadImage(image);
		return ApiResponse.ok(Map.of("image_url", imageUrl));
	}

	// POST /api/posts/caption
	@PostMapping("/caption")
	public ApiResponse<CaptionResponse> generateCaption(@RequestBody CaptionRequest request) {
		if (request.image_url() == null || request.business_type() == null || request.mood() == null) {
			throw ApiException.badRequest("image_url, business_type, mood는 필수예요.");
		}
		var result = geminiService.generateCaption(request.image_url(), request.business_type(), request.mood());
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

	// GET /api/posts — 히스토리 조회
	@GetMapping
	public ApiResponse<PostListResponse> listPosts(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestParam(required = false) String status,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int limit) {
		return ApiResponse.ok(postService.listPosts(principal.getId(), status, page, limit));
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
}
