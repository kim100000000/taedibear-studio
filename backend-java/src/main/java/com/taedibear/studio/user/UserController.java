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

	// PUT /api/users/me
	@PutMapping("/me")
	public ApiResponse<Map<String, Object>> updateMe(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestBody UpdateUserRequest request) {
		User user = userService.updateName(principal.getId(), request.name());
		return ApiResponse.ok(Map.of("id", user.getId(), "name", user.getName()));
	}
}
