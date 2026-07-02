package com.taedibear.studio.user;

import com.taedibear.studio.common.ApiResponse;
import com.taedibear.studio.domain.User;
import com.taedibear.studio.payment.PaymentService;
import com.taedibear.studio.payment.dto.UsageResponse;
import com.taedibear.studio.security.UserPrincipal;
import com.taedibear.studio.user.dto.MeResponse;
import com.taedibear.studio.user.dto.UpdateUserRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;
	private final PaymentService paymentService;

	// GET /api/users/me
	@GetMapping("/me")
	public ApiResponse<MeResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
		User user = userService.getById(principal.getId());
		return ApiResponse.ok(MeResponse.from(user));
	}

	// GET /api/users/me/usage — 플랜 + 이번 달 사용량 (Phase 2-1)
	@GetMapping("/me/usage")
	public ApiResponse<UsageResponse> usage(@AuthenticationPrincipal UserPrincipal principal) {
		return ApiResponse.ok(paymentService.getUsage(principal.getId()));
	}

	// POST /api/users/me/onboarding-complete — 온보딩 완료 +2 크레딧 (Phase 2-1)
	@PostMapping("/me/onboarding-complete")
	public ApiResponse<Map<String, Object>> onboardingComplete(
			@AuthenticationPrincipal UserPrincipal principal) {
		int credits = userService.completeOnboarding(principal.getId());
		return ApiResponse.ok(Map.of("credits", credits));
	}

	// POST /api/users/me/credits/ad-watch — 광고 시청 충전 +1 크레딧, 하루 2회 제한 (Phase 2-1)
	@PostMapping("/me/credits/ad-watch")
	public ApiResponse<Map<String, Object>> adWatch(
			@AuthenticationPrincipal UserPrincipal principal) {
		int credits = userService.watchAd(principal.getId());
		return ApiResponse.ok(Map.of("credits", credits));
	}

	// PUT /api/users/me
	@PutMapping("/me")
	public ApiResponse<Map<String, Object>> updateMe(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestBody UpdateUserRequest request) {
		User user = userService.updateName(principal.getId(), request.name());
		return ApiResponse.ok(Map.of("id", user.getId(), "name", user.getName()));
	}
}
