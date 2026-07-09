package com.taedibear.studio.post;

import com.taedibear.studio.common.ApiResponse;
import com.taedibear.studio.common.exception.ApiException;
import com.taedibear.studio.domain.SavedCaption;
import com.taedibear.studio.repository.SavedCaptionRepository;
import com.taedibear.studio.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 개선백로그 🟡: 캡션 보관함 (docs/05_API명세서.md).
 * POST /api/captions — 저장, GET — 목록, DELETE /:id — 삭제. 사용자당 최대 50개.
 */
@RestController
@RequestMapping("/api/captions")
@RequiredArgsConstructor
public class SavedCaptionController {

	private static final int MAX_SAVED = 50;

	private final SavedCaptionRepository savedCaptionRepository;

	public record SavedCaptionResponse(Long id, String caption, List<String> hashtags,
	                                   LocalDateTime created_at) {
		static SavedCaptionResponse from(SavedCaption s) {
			return new SavedCaptionResponse(s.getId(), s.getCaption(),
					HashtagUtil.toList(s.getHashtags()), s.getCreatedAt());
		}
	}

	public record SaveCaptionRequest(String caption, List<String> hashtags) {
	}

	@PostMapping
	@Transactional
	public ApiResponse<SavedCaptionResponse> save(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestBody SaveCaptionRequest request) {
		if (request.caption() == null || request.caption().isBlank()) {
			throw ApiException.badRequest("저장할 캡션이 없어요.");
		}
		if (savedCaptionRepository.countByUserId(principal.getId()) >= MAX_SAVED) {
			throw ApiException.badRequest("보관함이 가득 찼어요. (최대 " + MAX_SAVED + "개) 오래된 캡션을 삭제해주세요.");
		}
		SavedCaption saved = savedCaptionRepository.save(SavedCaption.builder()
				.userId(principal.getId())
				.caption(request.caption().trim())
				.hashtags(HashtagUtil.toStorageString(request.hashtags()))
				.build());
		return ApiResponse.ok(SavedCaptionResponse.from(saved));
	}

	@GetMapping
	public ApiResponse<List<SavedCaptionResponse>> list(@AuthenticationPrincipal UserPrincipal principal) {
		return ApiResponse.ok(savedCaptionRepository.findAllByUserIdOrderByCreatedAtDesc(principal.getId())
				.stream().map(SavedCaptionResponse::from).toList());
	}

	@DeleteMapping("/{id}")
	@Transactional
	public ApiResponse<Void> delete(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long id) {
		SavedCaption saved = savedCaptionRepository.findByIdAndUserId(id, principal.getId())
				.orElseThrow(() -> ApiException.notFound("저장된 캡션을 찾을 수 없어요."));
		savedCaptionRepository.delete(saved);
		return ApiResponse.ok();
	}
}
