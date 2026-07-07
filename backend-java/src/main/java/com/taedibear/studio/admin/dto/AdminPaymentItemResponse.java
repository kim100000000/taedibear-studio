package com.taedibear.studio.admin.dto;

import com.taedibear.studio.domain.Payment;

import java.time.LocalDateTime;

// Phase 5-1: 관리자 결제 목록 항목 (GET /api/admin/payments)
public record AdminPaymentItemResponse(
		Long id,
		Long user_id,
		String user_email,
		String order_id,
		int amount,
		String status,
		LocalDateTime valid_until,
		LocalDateTime created_at
) {
	public static AdminPaymentItemResponse from(Payment payment, String userEmail) {
		return new AdminPaymentItemResponse(payment.getId(), payment.getUserId(), userEmail,
				payment.getOrderId(), payment.getAmount(), payment.getStatus().name(),
				payment.getValidUntil(), payment.getCreatedAt());
	}
}
