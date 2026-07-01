package com.taedibear.studio.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentStatusResponse(
        String plan,          // "free" | "pro"
        String paymentKey,    // 최근 결제 키 (pro일 때만)
        LocalDateTime validUntil, // 구독 만료일 (pro일 때만)
        Integer amount        // 결제 금액 (pro일 때만)
) {}
