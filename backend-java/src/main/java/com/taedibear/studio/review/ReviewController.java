package com.taedibear.studio.review;

import com.taedibear.studio.common.ApiResponse;
import com.taedibear.studio.common.exception.ApiException;
import com.taedibear.studio.review.dto.CommentResponse;
import com.taedibear.studio.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Phase 4-3: 리뷰(댓글) 자동 답글 API
@RestController
@RequestMapping("/api/instagram")
@RequiredArgsConstructor
public class ReviewController {

	private final ReviewService reviewService;

	// GET /api/instagram/accounts/:id/comments — 최근 댓글 + AI 제안 답글 목록 (동기화 포함)
	@GetMapping("/accounts/{id}/comments")
	public ApiResponse<List<CommentResponse>> listComments(
			@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
		return ApiResponse.ok(reviewService.listComments(id, principal.getId()));
	}

	// POST /api/instagram/comments/:id/reply — 승인(또는 수정)한 답글을 실제로 발행
	@PostMapping("/comments/{id}/reply")
	public ApiResponse<Map<String, Boolean>> reply(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long id,
			@RequestBody Map<String, String> body) {
		String message = body.get("message");
		if (message == null || message.isBlank()) {
			throw ApiException.badRequest("답글 내용을 입력해주세요.");
		}
		reviewService.reply(id, principal.getId(), message);
		return ApiResponse.ok(Map.of("success", true));
	}

	// POST /api/instagram/comments/:id/skip — 이 댓글은 답글을 달지 않음
	@PostMapping("/comments/{id}/skip")
	public ApiResponse<Map<String, Boolean>> skip(
			@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
		reviewService.skip(id, principal.getId());
		return ApiResponse.ok(Map.of("success", true));
	}
}
