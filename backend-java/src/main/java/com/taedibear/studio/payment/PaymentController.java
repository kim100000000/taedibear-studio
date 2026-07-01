package com.taedibear.studio.payment;

import com.taedibear.studio.common.ApiResponse;
import com.taedibear.studio.payment.dto.*;
import com.taedibear.studio.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// Phase 2-1 결제 API (docs/05_API명세서.md 섹션 7)
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // POST /api/payments/confirm — 토스 결제 승인
    @PostMapping("/confirm")
    public ApiResponse<PaymentStatusResponse> confirm(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ConfirmPaymentRequest request) {
        return ApiResponse.ok(paymentService.confirm(principal.getId(), request));
    }

    // GET /api/payments/status — 현재 플랜 + 결제 상태
    @GetMapping("/status")
    public ApiResponse<PaymentStatusResponse> status(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(paymentService.getStatus(principal.getId()));
    }

    // POST /api/payments/cancel — 구독 해지
    @PostMapping("/cancel")
    public ApiResponse<Void> cancel(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CancelPaymentRequest request) {
        paymentService.cancel(principal.getId(), request);
        return ApiResponse.ok();
    }
}
