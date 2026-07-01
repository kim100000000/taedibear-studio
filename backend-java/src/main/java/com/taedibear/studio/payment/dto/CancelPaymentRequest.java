package com.taedibear.studio.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelPaymentRequest(
        @NotBlank String paymentKey,
        String cancelReason
) {}
