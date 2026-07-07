package com.taedibear.studio.admin;

import com.taedibear.studio.admin.dto.AdminPageResponse;
import com.taedibear.studio.admin.dto.AdminPaymentItemResponse;
import com.taedibear.studio.admin.dto.AdminSummaryResponse;
import com.taedibear.studio.admin.dto.AdminUserItemResponse;
import com.taedibear.studio.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Phase 5-1: 관리자 대시보드 API (docs/05_API명세서.md 관리자 섹션).
 * /api/admin/** 전체가 SecurityConfig에서 ROLE_ADMIN으로 보호된다.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

	private final AdminService adminService;

	// GET /api/admin/summary — 유저/게시물/예약/매출 요약
	@GetMapping("/summary")
	public ApiResponse<AdminSummaryResponse> summary() {
		return ApiResponse.ok(adminService.getSummary());
	}

	// GET /api/admin/users?page=&size=&search= — 유저 목록 (가입일 내림차순, 이메일/이름 검색)
	@GetMapping("/users")
	public ApiResponse<AdminPageResponse<AdminUserItemResponse>> users(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String search) {
		return ApiResponse.ok(adminService.getUsers(page, size, search));
	}

	// GET /api/admin/payments?page=&size= — 결제 내역 (최신순)
	@GetMapping("/payments")
	public ApiResponse<AdminPageResponse<AdminPaymentItemResponse>> payments(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.ok(adminService.getPayments(page, size));
	}
}
